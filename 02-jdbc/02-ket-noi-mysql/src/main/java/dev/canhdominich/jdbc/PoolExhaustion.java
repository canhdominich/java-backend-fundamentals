package dev.canhdominich.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.sql.Connection;
import java.sql.SQLException;

public class PoolExhaustion {

    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.fromEnvironment();

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.user());
        hikari.setPassword(config.password());
        hikari.setPoolName("lab-pool");
        hikari.setMaximumPoolSize(2);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(1000);
        hikari.setLeakDetectionThreshold(2000);

        try (HikariDataSource dataSource = new HikariDataSource(hikari)) {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            Connection first = dataSource.getConnection();
            Connection second = dataSource.getConnection();
            System.out.printf("borrowed 2: active=%d idle=%d waiting=%d%n", pool.getActiveConnections(), pool.getIdleConnections(), pool.getThreadsAwaitingConnection());

            long start = System.nanoTime();
            try (Connection third = dataSource.getConnection()) {
                System.out.println("third connection obtained (unexpected)");
            } catch (SQLException e) {
                System.out.printf("third borrow failed after %d ms%n", (System.nanoTime() - start) / 1_000_000);
                System.out.println("class  : " + e.getClass().getName());
                System.out.println("message: " + e.getMessage());
            }

            Thread.sleep(2500);
            first.close();
            second.close();
            System.out.printf("after close: active=%d idle=%d%n", pool.getActiveConnections(), pool.getIdleConnections());
        }
    }
}
