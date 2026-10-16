package dev.canhdominich.jdbc.crud;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public final class AffectedRows {

    private AffectedRows() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        for (String options : new String[]{"", "useAffectedRows=true"}) {
            try (Connection connection = options.isEmpty() ? Db.open(config) : Db.open(config, options)) {
                Db.resetSchema(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('KB-01', 'Keyboard', 49.90, 12)");
                }
                System.out.println((options.isEmpty() ? "default" : options)
                        + ": same value -> " + setStock(connection, 12)
                        + ", new value -> " + setStock(connection, 13)
                        + ", no such row -> " + setStockWhere(connection, 99, 5));
            }
        }
    }

    static int setStock(Connection connection, int stock) throws SQLException {
        return setStockWhere(connection, 1, stock);
    }

    static int setStockWhere(Connection connection, long id, int stock) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET stock = ? WHERE id = ?")) {
            ps.setInt(1, stock);
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }
}
