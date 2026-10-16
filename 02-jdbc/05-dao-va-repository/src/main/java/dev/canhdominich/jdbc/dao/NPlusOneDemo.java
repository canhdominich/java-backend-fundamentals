package dev.canhdominich.jdbc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

public final class NPlusOneDemo {

    static final int ORDERS = 200;

    private NPlusOneDemo() {
    }

    public static void main(String[] args) throws SQLException {
        try (var dataSource = DataSources.create(DbConfig.fromEnvironment(), 3)) {
            Schema.reset(dataSource);
            seedOrders(dataSource, ORDERS);
            OrderRepository orders = new OrderRepository(dataSource);
            try (Connection probe = dataSource.getConnection()) {
                long before = QueryCounter.questions(probe);
                List<OrderRepository.Order> a = orders.findAllOneQueryPerOrder();
                long afterOne = QueryCounter.questions(probe);
                List<OrderRepository.Order> b = orders.findAllWithJoin();
                long afterJoin = QueryCounter.questions(probe);
                System.out.println("one query per order : " + a.size() + " orders, about " + (afterOne - before - 1) + " statements");
                System.out.println("single JOIN         : " + b.size() + " orders, about " + (afterJoin - afterOne - 1) + " statements");
                System.out.println("same result         : " + a.equals(b));
            }
        }
    }

    static void seedOrders(DataSource dataSource, int count) throws SQLException {
        JdbcProductRepository products = new JdbcProductRepository(dataSource);
        long productId = products.insert("SKU-1", "Keyboard", java.math.BigDecimal.TEN, 1000).id();
        OrderRepository orders = new OrderRepository(dataSource);
        for (int i = 0; i < count; i++) {
            long orderId = orders.insertOrder("user" + i + "@example.com");
            orders.insertLine(orderId, productId, 1);
            orders.insertLine(orderId, productId, 2);
        }
    }
}
