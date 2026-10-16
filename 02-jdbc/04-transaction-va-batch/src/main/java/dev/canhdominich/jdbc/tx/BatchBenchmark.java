package dev.canhdominich.jdbc.tx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public final class BatchBenchmark {

    static final int ROWS = 2_000;
    private static final String INSERT = "INSERT INTO item(code, label) VALUES (?, ?)";

    private BatchBenchmark() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        report("one statement per row, autocommit on", time(config, "", Mode.AUTOCOMMIT_EACH));
        report("one statement per row, one transaction", time(config, "", Mode.TX_EACH));
        report("addBatch/executeBatch, one transaction", time(config, "", Mode.BATCH));
        report("batch + rewriteBatchedStatements=true", time(config, "rewriteBatchedStatements=true", Mode.BATCH));
    }

    enum Mode { AUTOCOMMIT_EACH, TX_EACH, BATCH }

    static void report(String label, long ms) {
        System.out.printf("%-42s %d rows in %d ms%n", label, ROWS, ms);
    }

    static long time(DbConfig config, String options, Mode mode) throws SQLException {
        try (Connection connection = options.isEmpty() ? Db.open(config) : Db.open(config, options)) {
            reset(connection);
            long start = System.nanoTime();
            insert(connection, mode, ROWS);
            return (System.nanoTime() - start) / 1_000_000;
        }
    }

    static void reset(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS item");
            statement.execute("CREATE TABLE item (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE, label VARCHAR(50) NOT NULL) ENGINE=InnoDB");
        }
    }

    static void insert(Connection connection, Mode mode, int rows) throws SQLException {
        if (mode != Mode.AUTOCOMMIT_EACH) {
            connection.setAutoCommit(false);
        }
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            for (int i = 0; i < rows; i++) {
                ps.setString(1, "C" + i);
                ps.setString(2, "item " + i);
                if (mode == Mode.BATCH) {
                    ps.addBatch();
                } else {
                    ps.executeUpdate();
                }
            }
            if (mode == Mode.BATCH) {
                ps.executeBatch();
            }
        }
        if (mode != Mode.AUTOCOMMIT_EACH) {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }
}
