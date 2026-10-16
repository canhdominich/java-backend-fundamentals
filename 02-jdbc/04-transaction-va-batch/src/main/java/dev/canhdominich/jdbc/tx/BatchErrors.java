package dev.canhdominich.jdbc.tx;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public final class BatchErrors {

    private BatchErrors() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        for (String options : new String[]{"", "rewriteBatchedStatements=true"}) {
            try (Connection connection = options.isEmpty() ? Db.open(config) : Db.open(config, options)) {
                System.out.println((options.isEmpty() ? "default" : options) + ": " + describe(connection));
            }
        }
    }

    static String describe(Connection connection) throws SQLException {
        BatchBenchmark.reset(connection);
        String[] codes = {"A", "B", "B", "C", "D"};
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO item(code, label) VALUES (?, ?)")) {
            for (String code : codes) {
                ps.setString(1, code);
                ps.setString(2, "label " + code);
                ps.addBatch();
            }
            int[] counts = ps.executeBatch();
            return "no error, counts " + Arrays.toString(counts) + ", rows stored " + count(connection);
        } catch (BatchUpdateException e) {
            return e.getClass().getSimpleName() + " counts " + Arrays.toString(e.getUpdateCounts()) + ", rows stored " + count(connection) + ", cause " + e.getMessage();
        }
    }

    private static int count(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var rs = statement.executeQuery("SELECT COUNT(*) FROM item")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
