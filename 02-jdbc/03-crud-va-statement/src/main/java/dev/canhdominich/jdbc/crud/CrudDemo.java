package dev.canhdominich.jdbc.crud;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CrudDemo {

    private CrudDemo() {
    }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = Db.open(DbConfig.fromEnvironment())) {
            Db.resetSchema(connection);
            long id = insert(connection, "KB-01", "Keyboard", new BigDecimal("49.90"), 12);
            System.out.println("generated id = " + id);
            System.out.println("found = " + findById(connection, id).orElseThrow());
            System.out.println("update rows = " + updatePrice(connection, id, new BigDecimal("59.90")));
            System.out.println("search = " + searchByMinPrice(connection, new BigDecimal("20.00")));
            System.out.println("delete rows = " + delete(connection, id));
            System.out.println("delete again rows = " + delete(connection, id));
        }
    }

    static long insert(Connection connection, String sku, String name, BigDecimal price, int stock) throws SQLException {
        String sql = "INSERT INTO product(sku, name, price, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sku);
            ps.setString(2, name);
            ps.setBigDecimal(3, price);
            ps.setInt(4, stock);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    static Optional<Product> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, sku, name, price, stock, created_at FROM product WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    static List<Product> searchByMinPrice(Connection connection, BigDecimal min) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, sku, name, price, stock, created_at FROM product WHERE price >= ? ORDER BY price")) {
            ps.setBigDecimal(1, min);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    static int updatePrice(Connection connection, long id, BigDecimal price) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET price = ? WHERE id = ?")) {
            ps.setBigDecimal(1, price);
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }

    static int delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    private static Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getBigDecimal("price"),
                rs.getInt("stock"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
