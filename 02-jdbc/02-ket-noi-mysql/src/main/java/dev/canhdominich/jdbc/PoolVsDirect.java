package dev.canhdominich.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

public class PoolVsDirect {

    private static final int ROUNDS = 200;

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();

        measureDirect(config);
        measurePooled(config);
    }

    static void measureDirect(DbConfig config) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
                ids.add(connectionId(connection));
            }
        }
        report("DriverManager", start, ids);
    }

    static void measurePooled(DbConfig config) throws SQLException {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.user());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(5);

        try (HikariDataSource dataSource = new HikariDataSource(hikari)) {
            Set<Long> ids = new LinkedHashSet<>();
            long start = System.nanoTime();
            for (int i = 0; i < ROUNDS; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    ids.add(connectionId(connection));
                }
            }
            report("HikariCP", start, ids);
        }
    }

    static long connectionId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT CONNECTION_ID()")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void report(String label, long startNanos, Set<Long> ids) {
        long millis = (System.nanoTime() - startNanos) / 1_000_000;
        System.out.printf("%-14s %d borrows in %d ms, distinct server connections: %d%n", label, ROUNDS, millis, ids.size());
    }
}
