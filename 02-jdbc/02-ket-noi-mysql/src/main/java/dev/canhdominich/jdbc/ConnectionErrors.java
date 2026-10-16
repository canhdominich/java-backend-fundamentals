package dev.canhdominich.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionErrors {

    public static void main(String[] args) {
        DbConfig good = DbConfig.fromEnvironment();

        report("Wrong password", good.withPassword("not-the-password"));
        report("Wrong port", good.withUrl(good.url().replace(":3307", ":3999")));
        report("Unknown database", good.withUrl(good.url().replace("/shop", "/no_such_db")));
        report("Wrong URL prefix", good.withUrl(good.url().replace("jdbc:mysql:", "jdbc:mysq:")));
    }

    static void report(String label, DbConfig config) {
        System.out.println("== " + label);
        try (var ignored = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            System.out.println("connected (unexpected)");
        } catch (SQLException e) {
            System.out.println("class    : " + e.getClass().getName());
            System.out.println("SQLState : " + e.getSQLState());
            System.out.println("errorCode: " + e.getErrorCode());
            System.out.println("message  : " + firstLine(e.getMessage()));
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            System.out.println("root     : " + root.getClass().getName() + ": " + firstLine(root.getMessage()));
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
