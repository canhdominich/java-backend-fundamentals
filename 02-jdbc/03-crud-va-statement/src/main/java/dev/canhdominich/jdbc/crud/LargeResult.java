package dev.canhdominich.jdbc.crud;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class LargeResult {

    static final int ROWS = 400_000;

    private LargeResult() {
    }

    public static void main(String[] args) throws SQLException {
        String mode = args.length > 0 ? args[0] : "default";
        DbConfig config = DbConfig.fromEnvironment();
        if (mode.equals("seed")) {
            seed(config);
            return;
        }
        if (mode.equals("blocked")) {
            blocked(config);
            return;
        }
        String options = mode.equals("cursor") ? "useCursorFetch=true" : "";
        long start = System.nanoTime();
        try (Connection connection = options.isEmpty() ? Db.open(config) : Db.open(config, options);
             PreparedStatement ps = connection.prepareStatement("SELECT id, payload FROM big_table", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            switch (mode) {
                case "stream" -> ps.setFetchSize(Integer.MIN_VALUE);
                case "cursor" -> ps.setFetchSize(1000);
                default -> {
                }
            }
            long count = 0;
            long bytes = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    bytes += rs.getString("payload").length();
                }
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println(mode + ": rows=" + count + " chars=" + bytes + " time=" + ms + " ms");
        } catch (SQLException e) {
            String cause = e.getCause() == null ? "none" : e.getCause().getClass().getSimpleName();
            System.out.println(mode + ": SQLException message=\"" + e.getMessage() + "\" cause=" + cause);
        }
    }

    static void blocked(DbConfig config) throws SQLException {
        try (Connection connection = Db.open(config);
             PreparedStatement ps = connection.prepareStatement("SELECT id FROM big_table", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                try (Statement other = connection.createStatement()) {
                    other.executeQuery("SELECT 1");
                } catch (SQLException e) {
                    System.out.println("second statement while streaming: " + e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " message=" + e.getMessage());
                }
            }
        }
    }

    static void seed(DbConfig config) throws SQLException {
        try (Connection connection = Db.open(config); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS big_table");
            statement.execute("CREATE TABLE big_table (id BIGINT PRIMARY KEY, payload VARCHAR(255) NOT NULL)");
            statement.execute("SET SESSION cte_max_recursion_depth = " + ROWS);
            statement.execute("INSERT INTO big_table SELECT n, REPEAT('x', 200) FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < " + ROWS + ") SELECT n FROM seq) s");
            try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM big_table")) {
                rs.next();
                System.out.println("seeded " + rs.getLong(1) + " rows");
            }
        }
    }
}
