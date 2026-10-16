package dev.canhdominich.jdbc.dao;

import java.sql.SQLException;
import javax.sql.DataSource;

public final class PartialFailureDemo {

    private PartialFailureDemo() {
    }

    public static void main(String[] args) throws SQLException {
        try (var dataSource = DataSources.create(DbConfig.fromEnvironment(), 3)) {
            Schema.reset(dataSource);
            JdbcProductRepository products = new JdbcProductRepository(dataSource);
            long id = products.insert("KB-01", "Keyboard", java.math.BigDecimal.TEN, 10).id();
            OrderService service = new OrderService(dataSource, products, new OrderRepository(dataSource));
            attempt("no transaction ", () -> service.placeOrderWithoutTransaction(null, id, 3), products);
            attempt("with transaction", () -> service.placeOrder(null, id, 3), products);
        }
    }

    static void attempt(String label, Runnable action, ProductRepository products) {
        int before = products.findBySku("KB-01").orElseThrow().stock();
        String outcome;
        try {
            action.run();
            outcome = "ok";
        } catch (DataAccessException e) {
            outcome = "DataAccessException(" + e.getMessage() + ") cause=" + e.getCause().getClass().getSimpleName();
        }
        System.out.println(label + " -> " + outcome + ", stock of KB-01 " + before + " -> " + products.findBySku("KB-01").orElseThrow().stock());
    }
}
