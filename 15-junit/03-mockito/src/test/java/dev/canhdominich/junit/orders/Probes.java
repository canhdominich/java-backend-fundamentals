package dev.canhdominich.junit.orders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

public final class Probes {

    private Probes() {
    }

    @ExtendWith(MockitoExtension.class)
    public static class StrictStubs {

        @Mock
        StockRepository stock;

        @Test
        void stubIsNeverUsed() {
            when(stock.available("BOOK")).thenReturn(10);
        }
    }

    @ExtendWith(MockitoExtension.class)
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    public static class LenientStubs {

        @Mock
        StockRepository stock;

        @Test
        void stubIsNeverUsed() {
            when(stock.available("BOOK")).thenReturn(10);
        }
    }

    public static class ForgotTheTax extends OrderService {

        private final PaymentGateway gateway;
        private final StockRepository repository;

        public ForgotTheTax(PaymentGateway gateway, StockRepository repository, Notifier notifier, TaxTable taxes) {
            super(gateway, repository, notifier, taxes);
            this.gateway = gateway;
            this.repository = repository;
        }

        @Override
        public String place(String customer, String country, String sku, int quantity, long unitPrice) {
            if (repository.available(sku) < quantity) {
                throw new IllegalStateException("out of stock: " + sku);
            }
            String transaction = gateway.charge(customer, unitPrice * quantity);
            repository.reserve(sku, quantity);
            return transaction;
        }
    }

    abstract static class ChargeChecks {

        abstract OrderService create(PaymentGateway payments, StockRepository stock, Notifier notifier, TaxTable taxes);

        PaymentGateway payments = mock(PaymentGateway.class);
        StockRepository stock = mock(StockRepository.class);
        Notifier notifier = mock(Notifier.class);
        TaxTable taxes = mock(TaxTable.class);

        void arrange() {
            when(stock.available("BOOK")).thenReturn(10);
            when(taxes.percentFor("VN")).thenReturn(10);
            when(payments.charge(anyString(), anyLong())).thenReturn("tx-1");
        }
    }

    public abstract static class LazyChecks extends ChargeChecks {

        @Test
        void chargesTheCustomer() {
            arrange();
            create(payments, stock, notifier, taxes).place("An", "VN", "BOOK", 2, 10_000);
            verify(payments).charge(anyString(), anyLong());
        }
    }

    public abstract static class PreciseChecks extends ChargeChecks {

        @Test
        void chargesThePriceWithTax() {
            arrange();
            create(payments, stock, notifier, taxes).place("An", "VN", "BOOK", 2, 10_000);
            verify(payments).charge(eq("An"), eq(22_000L));
        }
    }

    public static class LazyOnCorrect extends LazyChecks {

        @Override
        OrderService create(PaymentGateway payments, StockRepository stock, Notifier notifier, TaxTable taxes) {
            return new OrderService(payments, stock, notifier, taxes);
        }
    }

    public static class LazyOnBuggy extends LazyChecks {

        @Override
        OrderService create(PaymentGateway payments, StockRepository stock, Notifier notifier, TaxTable taxes) {
            return new ForgotTheTax(payments, stock, notifier, taxes);
        }
    }

    public static class PreciseOnCorrect extends PreciseChecks {

        @Override
        OrderService create(PaymentGateway payments, StockRepository stock, Notifier notifier, TaxTable taxes) {
            return new OrderService(payments, stock, notifier, taxes);
        }
    }

    public static class PreciseOnBuggy extends PreciseChecks {

        @Override
        OrderService create(PaymentGateway payments, StockRepository stock, Notifier notifier, TaxTable taxes) {
            return new ForgotTheTax(payments, stock, notifier, taxes);
        }
    }
}
