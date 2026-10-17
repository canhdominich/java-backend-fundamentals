package dev.canhdominich.hibernate.crud;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class SchemaDemo {

    private static DbConfig CONFIG;

    private SchemaDemo() {
    }

    public static void main(String[] args) throws SQLException {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) throws SQLException {
        CONFIG = config;
        System.out.println("-- no table at all");
        reset("DROP TABLE IF EXISTS student");
        attempt("none", "none");
        attempt("validate", "validate");
        System.out.println("-- an old table that lacks the column level and has an extra legacy_notes column");
        reset("DROP TABLE IF EXISTS student", "CREATE TABLE student (id BIGINT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(100) NOT NULL, email VARCHAR(100) NOT NULL UNIQUE, legacy_notes TEXT)");
        attempt("validate", "validate");
        attempt("update", "update");
        attempt("update again", "update");
        System.out.println("-- create and create-drop");
        attempt("create", "create");
        attempt("create-drop", "create-drop");
        reset("DROP TABLE IF EXISTS student");
    }

    static void attempt(String label, String mode) throws SQLException {
        try {
            SessionFactory factory = new Configuration()
                    .setProperty("hibernate.connection.url", CONFIG.url())
                    .setProperty("hibernate.connection.username", CONFIG.user())
                    .setProperty("hibernate.connection.password", CONFIG.password())
                    .setProperty("hibernate.hbm2ddl.auto", mode)
                    .addAnnotatedClass(Student.class)
                    .buildSessionFactory();
            factory.close();
            System.out.println(String.format("%-14s started, columns after: %s", label, columns()));
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            System.out.println(String.format("%-14s FAILED %s: %s", label, root.getClass().getSimpleName(), root.getMessage()));
        }
    }

    static List<String> columns() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection connection = open(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'student' ORDER BY ordinal_position")) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    static void reset(String... statements) throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(CONFIG.url(), CONFIG.user(), CONFIG.password());
    }
}
