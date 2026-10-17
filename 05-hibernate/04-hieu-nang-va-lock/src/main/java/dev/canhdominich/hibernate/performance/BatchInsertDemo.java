package dev.canhdominich.hibernate.performance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.function.IntFunction;
import org.hibernate.SessionFactory;

public class BatchInsertDemo {

    public record Result(String label, long millis, long insertCommands) {
    }

    public static void main(String[] args) throws SQLException {
        run(DbConfig.fromEnvironment(), 5000);
    }

    public static void run(DbConfig config, int rows) throws SQLException {
        Result identity = measure(config, "IDENTITY, batch_size=50", rows, Map.of("hibernate.jdbc.batch_size", "50"), i -> new MetricIdentity("m" + i, i));
        Result sequence = measure(config, "SEQUENCE, batch_size=50", rows, Map.of("hibernate.jdbc.batch_size", "50"), i -> new MetricSequence("m" + i, i));
        DbConfig rewrite = withRewrite(config);
        Result rewritten = measure(rewrite, "SEQUENCE, batch_size=50, rewriteBatchedStatements", rows, Map.of("hibernate.jdbc.batch_size", "50"), i -> new MetricSequence("m" + i, i));
        for (Result r : new Result[] {identity, sequence, rewritten}) {
            System.out.printf("%-58s %6d ms  %5d Com_insert%n", r.label(), r.millis(), r.insertCommands());
        }
    }

    static DbConfig withRewrite(DbConfig config) {
        return config.withUrl(config.url() + (config.url().contains("?") ? "&" : "?") + "rewriteBatchedStatements=true");
    }

    static Result measure(DbConfig config, String label, int rows, Map<String, String> extra, IntFunction<Object> factory) throws SQLException {
        try (SessionFactory sessions = Factories.build(config, extra)) {
            long before = comInsert(config);
            long start = System.nanoTime();
            UnitOfWork.run(sessions, session -> {
                for (int i = 1; i <= rows; i++) {
                    session.persist(factory.apply(i));
                    if (i % 50 == 0) {
                        session.flush();
                        session.clear();
                    }
                }
            });
            long millis = (System.nanoTime() - start) / 1_000_000;
            return new Result(label, millis, comInsert(config) - before);
        }
    }

    static long comInsert(DbConfig config) throws SQLException {
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("show global status like 'Com_insert'")) {
            rs.next();
            return rs.getLong(2);
        }
    }
}
