package dev.canhdominich.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MaxConnectionsDemo {

    public static void main(String[] args) throws SQLException {
        DbConfig app = DbConfig.fromEnvironment();
        String rootPassword = System.getenv().getOrDefault("DB_ROOT_PASSWORD", "root_pw");
        try (Connection admin = DriverManager.getConnection(app.url(), "root", rootPassword);
             Statement statement = admin.createStatement()) {
            statement.execute("SET GLOBAL max_connections = 8");
            List<Connection> opened = new ArrayList<>();
            try {
                for (int i = 1; i <= 20; i++) {
                    opened.add(DriverManager.getConnection(app.url(), app.user(), app.password()));
                }
            } catch (SQLException e) {
                System.out.println("opened " + opened.size() + " connections, then " + e.getClass().getSimpleName()
                        + " SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " message=" + e.getMessage());
            } finally {
                for (Connection connection : opened) {
                    connection.close();
                }
                statement.execute("SET GLOBAL max_connections = 151");
            }
        }
    }
}
