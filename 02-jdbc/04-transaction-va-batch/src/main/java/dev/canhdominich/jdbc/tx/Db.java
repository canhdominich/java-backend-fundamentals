package dev.canhdominich.jdbc.tx;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

final class Db {

    private Db() {
    }

    static Connection open(DbConfig config) throws SQLException {
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    static Connection open(DbConfig config, String urlOptions) throws SQLException {
        String separator = config.url().contains("?") ? "&" : "?";
        return DriverManager.getConnection(config.url() + separator + urlOptions, config.user(), config.password());
    }

    static void resetAccounts(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS account");
            statement.execute("""
                    CREATE TABLE account (
                        id BIGINT PRIMARY KEY,
                        owner VARCHAR(50) NOT NULL,
                        balance DECIMAL(12,2) NOT NULL,
                        CONSTRAINT balance_not_negative CHECK (balance >= 0)
                    ) ENGINE=InnoDB""");
            statement.execute("INSERT INTO account(id, owner, balance) VALUES (1, 'An', 100.00), (2, 'Binh', 50.00)");
        }
    }

    static java.math.BigDecimal balance(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT balance FROM account WHERE id = ?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}
