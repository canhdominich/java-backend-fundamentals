package dev.canhdominich.jdbc.crud;

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

    static void resetSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS product");
            statement.execute("DROP TABLE IF EXISTS app_user");
            statement.execute("DROP TABLE IF EXISTS audit_log");
            statement.execute("""
                    CREATE TABLE product (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        sku VARCHAR(32) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        stock INT NOT NULL DEFAULT 0,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )""");
            statement.execute("""
                    CREATE TABLE app_user (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        password_hash VARCHAR(100) NOT NULL,
                        role VARCHAR(20) NOT NULL
                    )""");
            statement.execute("CREATE TABLE audit_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, message VARCHAR(200) NOT NULL)");
            statement.execute("INSERT INTO app_user(username, password_hash, role) VALUES ('admin', 'hash-admin', 'ADMIN'), ('an', 'hash-an', 'USER')");
            statement.execute("INSERT INTO audit_log(message) VALUES ('first entry')");
        }
    }
}
