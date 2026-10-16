package dev.canhdominich.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UrlOptions {

    private static final String[] OPTIONS = {"", "useSSL=false", "sslMode=DISABLED", "sslMode=REQUIRED", "sslMode=VERIFY_CA", "useSSl=false"};

    public static void main(String[] args) {
        DbConfig config = DbConfig.fromEnvironment();
        for (String option : OPTIONS) {
            System.out.printf("%-20s %s%n", option.isEmpty() ? "(no option)" : option, cipher(config, option));
        }
    }

    static String cipher(DbConfig config, String option) {
        String url = option.isEmpty() ? config.url() : config.url() + "?" + option;
        try (Connection connection = DriverManager.getConnection(url, config.user(), config.password());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
            rs.next();
            String cipher = rs.getString(2);
            return cipher.isEmpty() ? "no encryption" : cipher;
        } catch (SQLException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            return "FAILED " + root.getClass().getSimpleName();
        }
    }
}
