package dev.canhdominich.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PublicKeyRetrieval {

    public static void main(String[] args) {
        DbConfig config = DbConfig.fromEnvironment();
        System.out.println("1. sslMode=DISABLED                           : " + attempt(config, "sslMode=DISABLED"));
        System.out.println("2. sslMode=DISABLED&allowPublicKeyRetrieval=true: " + attempt(config, "sslMode=DISABLED&allowPublicKeyRetrieval=true"));
        System.out.println("3. sslMode=DISABLED again                      : " + attempt(config, "sslMode=DISABLED"));
    }

    static String attempt(DbConfig config, String options) {
        try (Connection ignored = DriverManager.getConnection(config.url() + "?" + options, config.user(), config.password())) {
            return "connected";
        } catch (SQLException e) {
            return e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " " + e.getMessage();
        }
    }
}
