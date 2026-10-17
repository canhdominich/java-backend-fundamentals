package dev.canhdominich.junit.orders;

public class OrderService {

    private final PaymentGateway payments;
    private final StockRepository stock;
    private final Notifier notifier;
    private final TaxTable taxes;

    public OrderService(PaymentGateway payments, StockRepository stock, Notifier notifier, TaxTable taxes) {
        this.payments = payments;
        this.stock = stock;
        this.notifier = notifier;
        this.taxes = taxes;
    }

    public String place(String customer, String country, String sku, int quantity, long unitPrice) {
        if (stock.available(sku) < quantity) {
            throw new IllegalStateException("out of stock: " + sku);
        }
        long net = unitPrice * quantity;
        long gross = net + net * taxes.percentFor(country) / 100;
        String transaction;
        try {
            transaction = payments.charge(customer, gross);
        } catch (RuntimeException e) {
            notifier.send(customer, "payment failed");
            throw e;
        }
        try {
            stock.reserve(sku, quantity);
        } catch (RuntimeException e) {
            payments.refund(transaction);
            throw e;
        }
        notifier.send(customer, "order confirmed: " + transaction);
        return transaction;
    }
}
