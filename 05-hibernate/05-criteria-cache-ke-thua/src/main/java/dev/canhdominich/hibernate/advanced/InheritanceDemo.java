package dev.canhdominich.hibernate.advanced;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class InheritanceDemo {

    public static void main(String[] args) throws SQLException {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) throws SQLException {
        singleTable(config);
        joined(config);
        tablePerClass(config);
        identityWithTablePerClass(config);
    }

    static List<String> singleTable(DbConfig config) throws SQLException {
        try (SessionFactory factory = Factories.build(config,
                dev.canhdominich.hibernate.advanced.single.Payment.class,
                dev.canhdominich.hibernate.advanced.single.CardPayment.class,
                dev.canhdominich.hibernate.advanced.single.TransferPayment.class)) {
            return explore(config, factory, "SINGLE_TABLE", List.of("payment"), session -> {
                session.persist(new dev.canhdominich.hibernate.advanced.single.CardPayment(150_000, "4242"));
                session.persist(new dev.canhdominich.hibernate.advanced.single.TransferPayment(300_000, "FT-001"));
            });
        }
    }

    static List<String> joined(DbConfig config) throws SQLException {
        try (SessionFactory factory = Factories.build(config,
                dev.canhdominich.hibernate.advanced.joined.Payment.class,
                dev.canhdominich.hibernate.advanced.joined.CardPayment.class,
                dev.canhdominich.hibernate.advanced.joined.TransferPayment.class)) {
            return explore(config, factory, "JOINED", List.of("payment", "card_payment", "transfer_payment"), session -> {
                session.persist(new dev.canhdominich.hibernate.advanced.joined.CardPayment(150_000, "4242"));
                session.persist(new dev.canhdominich.hibernate.advanced.joined.TransferPayment(300_000, "FT-001"));
            });
        }
    }

    static List<String> tablePerClass(DbConfig config) throws SQLException {
        try (SessionFactory factory = Factories.build(config,
                dev.canhdominich.hibernate.advanced.tpc.Payment.class,
                dev.canhdominich.hibernate.advanced.tpc.CardPayment.class,
                dev.canhdominich.hibernate.advanced.tpc.TransferPayment.class)) {
            return explore(config, factory, "TABLE_PER_CLASS", List.of("card_payment", "transfer_payment"), session -> {
                session.persist(new dev.canhdominich.hibernate.advanced.tpc.CardPayment(150_000, "4242"));
                session.persist(new dev.canhdominich.hibernate.advanced.tpc.TransferPayment(300_000, "FT-001"));
            });
        }
    }

    static String identityWithTablePerClass(DbConfig config) {
        try (SessionFactory ignored = Factories.build(config,
                dev.canhdominich.hibernate.advanced.tpc.IdentityBase.class,
                dev.canhdominich.hibernate.advanced.tpc.IdentityChild.class)) {
            return "no error";
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String message = root.getClass().getSimpleName() + ": " + root.getMessage();
            System.out.println("== TABLE_PER_CLASS with IDENTITY ==");
            System.out.println("  " + message);
            return message;
        }
    }

    private static List<String> explore(DbConfig config, SessionFactory factory, String label, List<String> tables, Consumer<Session> insert) throws SQLException {
        System.out.println("== " + label + " ==");
        List<String> lines = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
                Statement statement = connection.createStatement()) {
            for (String table : tables) {
                try (ResultSet rs = statement.executeQuery("select column_name, column_type, is_nullable from information_schema.columns where table_schema = database() and table_name = '" + table + "' order by column_name")) {
                    StringBuilder columns = new StringBuilder();
                    while (rs.next()) {
                        columns.append(' ').append(rs.getString(1)).append(' ').append("YES".equals(rs.getString(3)) ? "null" : "not-null").append(',');
                    }
                    String line = "table " + table + ":" + columns;
                    System.out.println("  " + line);
                    lines.add(line);
                }
            }
        }
        SqlRecorder.clear();
        UnitOfWork.run(factory, insert);
        SqlRecorder.print("  inserts");
        List<?> all = UnitOfWork.inTransaction(factory, session -> session.createQuery("from Payment", Object.class).getResultList());
        System.out.println("  from Payment: " + all.size() + " rows");
        SqlRecorder.print("  SQL of the polymorphic query");
        UnitOfWork.run(factory, session -> session.createQuery("from CardPayment", Object.class).getResultList());
        SqlRecorder.print("  SQL of the query on one subclass");
        return lines;
    }
}
