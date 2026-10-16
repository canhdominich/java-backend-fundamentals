package dev.canhdominich.jdbc.crud;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class OrderByAndIn {

    private static final Map<String, String> SORTABLE = Map.of("price", "price", "name", "name", "stock", "stock");

    private OrderByAndIn() {
    }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = Db.open(DbConfig.fromEnvironment())) {
            Db.resetSchema(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('A', 'Charger', 30.00, 5), ('B', 'Adapter', 10.00, 9), ('C', 'Battery', 20.00, 1)");
            }
            System.out.println("ORDER BY ? with 'price' : " + names(connection, "SELECT name FROM product ORDER BY ?", "price"));
            System.out.println("ORDER BY price           : " + names(connection, "SELECT name FROM product ORDER BY price", null));
            System.out.println("whitelist sort=name      : " + names(connection, "SELECT name FROM product ORDER BY " + sortColumn("name"), null));
            try {
                sortColumn("price; DROP TABLE product");
            } catch (IllegalArgumentException e) {
                System.out.println("whitelist rejects        : " + e.getMessage());
            }
            System.out.println("IN list                  : " + findBySkus(connection, List.of("A", "C")));
        }
    }

    static String sortColumn(String requested) {
        String column = SORTABLE.get(requested);
        if (column == null) {
            throw new IllegalArgumentException("unsupported sort column: " + requested);
        }
        return column;
    }

    static List<String> findBySkus(Connection connection, List<String> skus) throws SQLException {
        String placeholders = String.join(", ", Collections.nCopies(skus.size(), "?"));
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM product WHERE sku IN (" + placeholders + ") ORDER BY name")) {
            for (int i = 0; i < skus.size(); i++) {
                ps.setString(i + 1, skus.get(i));
            }
            return readNames(ps);
        }
    }

    static List<String> names(Connection connection, String sql, String parameter) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            return readNames(ps);
        }
    }

    private static List<String> readNames(PreparedStatement ps) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }
}
