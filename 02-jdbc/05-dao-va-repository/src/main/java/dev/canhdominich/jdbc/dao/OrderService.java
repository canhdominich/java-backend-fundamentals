package dev.canhdominich.jdbc.dao;

import javax.sql.DataSource;

public final class OrderService {

    private final DataSource dataSource;
    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(DataSource dataSource, ProductRepository products, OrderRepository orders) {
        this.dataSource = dataSource;
        this.products = products;
        this.orders = orders;
    }

    public long placeOrderWithoutTransaction(String email, long productId, int quantity) {
        return place(email, productId, quantity);
    }

    public long placeOrder(String email, long productId, int quantity) {
        return TransactionContext.inTransaction(dataSource, () -> place(email, productId, quantity));
    }

    private long place(String email, long productId, int quantity) {
        if (!products.decreaseStock(productId, quantity)) {
            throw new IllegalStateException("not enough stock for product " + productId);
        }
        long orderId = orders.insertOrder(email);
        orders.insertLine(orderId, productId, quantity);
        return orderId;
    }
}
