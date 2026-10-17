package dev.canhdominich.junit.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    PaymentGateway payments;

    @Mock
    StockRepository stock;

    @Mock
    Notifier notifier;

    @Mock
    TaxTable taxes;

    OrderService service;

    @BeforeEach
    void createService() {
        service = new OrderService(payments, stock, notifier, taxes);
    }

    @Test
    @DisplayName("charges the price plus tax, reserves the stock and confirms")
    void happyPath() {
        when(stock.available("BOOK")).thenReturn(10);
        when(taxes.percentFor("VN")).thenReturn(10);
        when(payments.charge(anyString(), anyLong())).thenReturn("tx-1");

        String transaction = service.place("An", "VN", "BOOK", 2, 10_000);

        assertThat(transaction).isEqualTo("tx-1");
        ArgumentCaptor<Long> amount = ArgumentCaptor.forClass(Long.class);
        verify(payments).charge(org.mockito.ArgumentMatchers.eq("An"), amount.capture());
        assertThat(amount.getValue()).isEqualTo(22_000L);
        verify(stock).reserve("BOOK", 2);
        verify(notifier).send("An", "order confirmed: tx-1");
    }

    @Test
    @DisplayName("does not touch the gateway when the stock is short")
    void outOfStock() {
        when(stock.available("BOOK")).thenReturn(1);

        assertThatThrownBy(() -> service.place("An", "VN", "BOOK", 2, 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("out of stock: BOOK");

        verifyNoInteractions(payments, notifier);
    }

    @Test
    @DisplayName("tells the customer and reserves nothing when the payment fails")
    void paymentFails() {
        when(stock.available("BOOK")).thenReturn(10);
        when(taxes.percentFor("VN")).thenReturn(10);
        when(payments.charge(anyString(), anyLong())).thenThrow(new IllegalStateException("card declined"));

        assertThatThrownBy(() -> service.place("An", "VN", "BOOK", 2, 10_000)).hasMessage("card declined");

        verify(notifier).send("An", "payment failed");
        verify(stock, never()).reserve(anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("refunds the payment when the stock cannot be reserved")
    void reserveFails() {
        when(stock.available("BOOK")).thenReturn(10);
        when(taxes.percentFor("VN")).thenReturn(10);
        when(payments.charge(anyString(), anyLong())).thenReturn("tx-2");
        doThrow(new IllegalStateException("conflict")).when(stock).reserve("BOOK", 2);

        assertThatThrownBy(() -> service.place("An", "VN", "BOOK", 2, 10_000)).hasMessage("conflict");

        verify(payments).refund("tx-2");
        verify(notifier, never()).send(anyString(), anyString());
    }
}
