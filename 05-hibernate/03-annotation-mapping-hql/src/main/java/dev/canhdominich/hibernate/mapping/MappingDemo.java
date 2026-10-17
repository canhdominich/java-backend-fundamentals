package dev.canhdominich.hibernate.mapping;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.SessionFactory;

public final class MappingDemo {

    private MappingDemo() {
    }

    public static void main(String[] args) throws SQLException {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) throws SQLException {
        try (SessionFactory factory = Factories.build(config, false); Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            System.out.println("-- table names with Hibernate's default naming: " + tables(connection));
        }
        try (SessionFactory factory = Factories.build(config)) {
            try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
                System.out.println("-- table names with CamelCaseToUnderscoresNamingStrategy: " + tables(connection));
                for (String table : new String[]{"student", "enrollment", "student_course", "classroom"}) {
                    printTable(connection, table);
                }
                UnitOfWork.run(factory, session -> session.persist(new Grade(Grade.Mark.MERIT)));
                try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT ordinal_mark, string_mark FROM grade")) {
                    rs.next();
                    System.out.println("-- enum stored as ORDINAL: " + rs.getString(1) + ", as STRING: " + rs.getString(2));
                }
            }
        }
    }

    static java.util.List<String> tables(Connection connection) throws SQLException {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('classroom', 'course', 'enrollment', 'grade', 'student', 'student_course') ORDER BY table_name")) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    static void printTable(Connection connection, String table) throws SQLException {
        System.out.println("-- columns and constraints of " + table);
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("SELECT column_name, column_type, is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '" + table + "' ORDER BY ordinal_position")) {
            while (columns.next()) {
                System.out.println("  " + columns.getString(1) + " " + columns.getString(2) + (columns.getString(3).equals("NO") ? " NOT NULL" : ""));
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet keys = statement.executeQuery("SELECT column_name, referenced_table_name FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = '" + table + "' AND referenced_table_name IS NOT NULL ORDER BY column_name")) {
            while (keys.next()) {
                System.out.println("  foreign key " + keys.getString(1) + " -> " + keys.getString(2));
            }
        }
    }
}
