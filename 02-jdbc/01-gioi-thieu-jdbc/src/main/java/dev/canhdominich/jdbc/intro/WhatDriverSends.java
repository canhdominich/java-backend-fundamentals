package dev.canhdominich.jdbc.intro;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class WhatDriverSends {

    private WhatDriverSends() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig app = DbConfig.fromEnvironment();
        DbConfig root = new DbConfig(app.url(), "root", System.getenv().getOrDefault("DB_ROOT_PASSWORD", "root_pw"));
        for (String line : trace(app, root)) {
            System.out.println(line);
        }
    }

    static List<String> trace(DbConfig app, DbConfig root) throws SQLException {
        List<String> lines = new ArrayList<>();
        try (Connection admin = DriverManager.getConnection(root.url(), root.user(), root.password())) {
            try (Statement statement = admin.createStatement()) {
                statement.execute("SET GLOBAL log_output = 'TABLE'");
                statement.execute("SET GLOBAL general_log = 'ON'");
            }
            long connectionId;
            try (Connection connection = DriverManager.getConnection(app.url(), app.user(), app.password())) {
                connectionId = connectionId(connection);
                try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM product WHERE sku = ?")) {
                    ps.setString(1, "KB-01");
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                    }
                }
            }
            try (Statement statement = admin.createStatement()) {
                statement.execute("SET GLOBAL general_log = 'OFF'");
                try (ResultSet rs = statement.executeQuery(
                        "SELECT command_type, CONVERT(argument USING utf8mb4) FROM mysql.general_log WHERE thread_id = " + connectionId + " ORDER BY event_time")) {
                    while (rs.next()) {
                        lines.add(rs.getString(1) + "  " + rs.getString(2));
                    }
                }
                statement.execute("TRUNCATE mysql.general_log");
            }
        }
        return lines;
    }

    private static long connectionId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT CONNECTION_ID()")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
