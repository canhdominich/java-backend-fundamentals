package dev.canhdominich.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HelloConnection {

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();

        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            var meta = connection.getMetaData();
            System.out.println("Connected to " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("Driver: " + meta.getDriverName() + " " + meta.getDriverVersion());
            System.out.println("Catalog: " + connection.getCatalog());
            System.out.println("isValid(2): " + connection.isValid(2));

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT CONNECTION_ID(), CURRENT_USER()")) {
                rs.next();
                System.out.println("Server connection id: " + rs.getLong(1));
                System.out.println("Authenticated as: " + rs.getString(2));
            }
        }
    }
}
