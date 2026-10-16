package dev.canhdominich.jdbc.crud;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqlInjectionDemo {

    private SqlInjectionDemo() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        try (Connection connection = Db.open(config)) {
            Db.resetSchema(connection);
            String[][] attempts = {
                    {"an", "hash-an"},
                    {"an", "wrong"},
                    {"admin' -- ", "anything"},
                    {"nobody", "x' OR '1'='1"}
            };
            for (String[] attempt : attempts) {
                System.out.println("login(" + attempt[0] + " | " + attempt[1] + ")  concat=" + vulnerableLogin(connection, attempt[0], attempt[1])
                        + "  prepared=" + safeLogin(connection, attempt[0], attempt[1]));
            }
        }
        stacked(config, "default URL");
        stacked(config, "allowMultiQueries=true");
    }

    static String vulnerableLogin(Connection connection, String username, String password) throws SQLException {
        String sql = "SELECT role FROM app_user WHERE username = '" + username + "' AND password_hash = '" + password + "'";
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? "LOGGED_IN as " + rs.getString(1) : "denied";
        } catch (SQLException e) {
            return "error " + e.getSQLState();
        }
    }

    static String safeLogin(Connection connection, String username, String password) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT role FROM app_user WHERE username = ? AND password_hash = ?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? "LOGGED_IN as " + rs.getString(1) : "denied";
            }
        }
    }

    static String stacked(DbConfig config, String label) throws SQLException {
        String outcome;
        try (Connection connection = label.startsWith("allow") ? Db.open(config, "allowMultiQueries=true") : Db.open(config)) {
            Db.resetSchema(connection);
            String input = "x'; DROP TABLE audit_log; -- ";
            String sql = "SELECT role FROM app_user WHERE username = '" + input + "'";
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                outcome = "query ran";
            } catch (SQLException e) {
                outcome = "rejected " + e.getClass().getSimpleName() + " SQLState=" + e.getSQLState();
            }
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'audit_log'")) {
                rs.next();
                outcome += ", audit_log exists = " + (rs.getInt(1) == 1);
            }
        }
        System.out.println("stacked query with " + label + ": " + outcome);
        return outcome;
    }
}
