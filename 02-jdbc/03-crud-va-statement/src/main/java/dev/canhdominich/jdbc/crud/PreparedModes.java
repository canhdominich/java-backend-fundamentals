package dev.canhdominich.jdbc.crud;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class PreparedModes {

    static final int ROUNDS = 5000;

    private PreparedModes() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        try (Connection connection = Db.open(config)) {
            Db.resetSchema(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('KB-01', 'Keyboard', 49.90, 12)");
            }
        }
        String[] modes = {"", "useServerPrepStmts=true", "useServerPrepStmts=true&cachePrepStmts=true"};
        for (String mode : modes) {
            try (Connection connection = mode.isEmpty() ? Db.open(config) : Db.open(config, mode)) {
                run(connection, 200);
                long start = System.nanoTime();
                run(connection, ROUNDS);
                long ms = (System.nanoTime() - start) / 1_000_000;
                System.out.printf("%-48s %d queries in %d ms%n", mode.isEmpty() ? "default (client-side)" : mode, ROUNDS, ms);
            }
        }
    }

    static void run(Connection connection, int rounds) throws SQLException {
        for (int i = 0; i < rounds; i++) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM product WHERE sku = ?")) {
                ps.setString(1, "KB-01");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                }
            }
        }
    }
}
