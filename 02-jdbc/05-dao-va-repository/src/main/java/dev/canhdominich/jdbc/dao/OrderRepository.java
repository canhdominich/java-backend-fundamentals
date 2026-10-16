package dev.canhdominich.jdbc.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

public final class OrderRepository {

    public record Line(long productId, int quantity) {
    }

    public record Order(long id, String customerEmail, List<Line> lines) {
    }

    private final DataSource dataSource;

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long insertOrder(String customerEmail) {
        return TransactionContext.withConnection(dataSource, "insert order", connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO shop_order(customer_email) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, customerEmail);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    public void insertLine(long orderId, long productId, int quantity) {
        TransactionContext.withConnection(dataSource, "insert line", connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO order_line(order_id, product_id, quantity) VALUES (?, ?, ?)")) {
                ps.setLong(1, orderId);
                ps.setLong(2, productId);
                ps.setInt(3, quantity);
                return ps.executeUpdate();
            }
        });
    }

    public List<Order> findAllOneQueryPerOrder() {
        return TransactionContext.withConnection(dataSource, "orders, one query per order", connection -> {
            List<Order> orders = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT id, customer_email FROM shop_order ORDER BY id"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    orders.add(new Order(id, rs.getString("customer_email"), linesOf(connection, id)));
                }
            }
            return orders;
        });
    }

    public List<Order> findAllWithJoin() {
        return TransactionContext.withConnection(dataSource, "orders with join", connection -> {
            Map<Long, Order> byId = new LinkedHashMap<>();
            String sql = "SELECT o.id, o.customer_email, l.product_id, l.quantity FROM shop_order o LEFT JOIN order_line l ON l.order_id = o.id ORDER BY o.id, l.id";
            try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("o.id");
                    Order order = byId.computeIfAbsent(id, key -> {
                        try {
                            return new Order(key, rs.getString("o.customer_email"), new ArrayList<>());
                        } catch (java.sql.SQLException e) {
                            throw new DataAccessException("map order", e);
                        }
                    });
                    long productId = rs.getLong("l.product_id");
                    if (!rs.wasNull()) {
                        order.lines().add(new Line(productId, rs.getInt("l.quantity")));
                    }
                }
            }
            return new ArrayList<>(byId.values());
        });
    }

    private static List<Line> linesOf(java.sql.Connection connection, long orderId) throws java.sql.SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT product_id, quantity FROM order_line WHERE order_id = ? ORDER BY id")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Line> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(new Line(rs.getLong("product_id"), rs.getInt("quantity")));
                }
                return lines;
            }
        }
    }
}
