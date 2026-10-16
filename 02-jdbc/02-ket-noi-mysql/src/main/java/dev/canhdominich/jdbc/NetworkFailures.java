package dev.canhdominich.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;

public class NetworkFailures {

    public static void main(String[] args) {
        DbConfig good = DbConfig.fromEnvironment();
        report("Unknown host", "jdbc:mysql://no-such-host.invalid:3307/shop", good, 0);
        report("Closed port", good.url().replace(":3307", ":3999"), good, 0);
        report("Unroutable address, connectTimeout=2000", "jdbc:mysql://10.255.255.1:3307/shop?connectTimeout=2000", good, 0);
        report("Unroutable address, loginTimeout=1 second", "jdbc:mysql://10.255.255.1:3307/shop", good, 1);
    }

    static void report(String label, String url, DbConfig credentials, int loginTimeoutSeconds) {
        DriverManager.setLoginTimeout(loginTimeoutSeconds);
        long start = System.nanoTime();
        try (var ignored = DriverManager.getConnection(url, credentials.user(), credentials.password())) {
            System.out.println(label + ": connected (unexpected)");
        } catch (SQLException e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            System.out.printf("%-44s %s SQLState=%s root=%s: %s after %d ms%n", label, e.getClass().getSimpleName(), e.getSQLState(),
                    root.getClass().getSimpleName(), root.getMessage(), ms);
        } finally {
            DriverManager.setLoginTimeout(0);
        }
    }
}
