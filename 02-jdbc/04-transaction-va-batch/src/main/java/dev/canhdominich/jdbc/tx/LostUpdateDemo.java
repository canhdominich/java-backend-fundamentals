package dev.canhdominich.jdbc.tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class LostUpdateDemo {

    private LostUpdateDemo() {
    }

    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.fromEnvironment();
        System.out.println("read then write absolute value : final balance " + readThenWrite(config));
        System.out.println("atomic UPDATE balance = balance - ? : final balance " + atomic(config));
        long[] waited = new long[1];
        System.out.println("SELECT ... FOR UPDATE           : final balance " + forUpdate(config, waited) + ", second writer waited >= 500 ms: " + (waited[0] >= 500));
        System.out.println("optimistic version column       : " + optimistic(config));
    }

    static BigDecimal readThenWrite(DbConfig config) throws SQLException {
        try (Connection setup = Db.open(config); Connection a = Db.open(config); Connection b = Db.open(config)) {
            Db.resetAccounts(setup);
            a.setAutoCommit(false);
            b.setAutoCommit(false);
            BigDecimal readA = Db.balance(a, 1);
            BigDecimal readB = Db.balance(b, 1);
            setBalance(a, 1, readA.subtract(new BigDecimal("10.00")));
            a.commit();
            setBalance(b, 1, readB.subtract(new BigDecimal("20.00")));
            b.commit();
            return Db.balance(setup, 1);
        }
    }

    static BigDecimal atomic(DbConfig config) throws SQLException {
        try (Connection setup = Db.open(config); Connection a = Db.open(config); Connection b = Db.open(config)) {
            Db.resetAccounts(setup);
            a.setAutoCommit(false);
            b.setAutoCommit(false);
            TransferDemo.update(a, 1, new BigDecimal("-10.00"));
            a.commit();
            TransferDemo.update(b, 1, new BigDecimal("-20.00"));
            b.commit();
            return Db.balance(setup, 1);
        }
    }

    static BigDecimal forUpdate(DbConfig config, long[] waited) throws Exception {
        try (Connection setup = Db.open(config); Connection a = Db.open(config); Connection b = Db.open(config)) {
            Db.resetAccounts(setup);
            a.setAutoCommit(false);
            b.setAutoCommit(false);
            BigDecimal readA = lockAndRead(a, 1);
            Thread second = new Thread(() -> {
                try {
                    long start = System.nanoTime();
                    BigDecimal readB = lockAndRead(b, 1);
                    waited[0] = (System.nanoTime() - start) / 1_000_000;
                    setBalance(b, 1, readB.subtract(new BigDecimal("20.00")));
                    b.commit();
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            second.start();
            Thread.sleep(500);
            setBalance(a, 1, readA.subtract(new BigDecimal("10.00")));
            a.commit();
            second.join();
            return Db.balance(setup, 1);
        }
    }

    static String optimistic(DbConfig config) throws SQLException {
        try (Connection connection = Db.open(config); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS account_v");
            statement.execute("CREATE TABLE account_v (id BIGINT PRIMARY KEY, balance DECIMAL(12,2) NOT NULL, version INT NOT NULL)");
            statement.execute("INSERT INTO account_v VALUES (1, 100.00, 0)");
            int first = updateVersioned(connection, 1, new BigDecimal("90.00"), 0);
            int second = updateVersioned(connection, 1, new BigDecimal("80.00"), 0);
            return "first writer rows=" + first + ", second writer (stale version) rows=" + second;
        }
    }

    static int updateVersioned(Connection connection, long id, BigDecimal balance, int expectedVersion) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE account_v SET balance = ?, version = version + 1 WHERE id = ? AND version = ?")) {
            ps.setBigDecimal(1, balance);
            ps.setLong(2, id);
            ps.setInt(3, expectedVersion);
            return ps.executeUpdate();
        }
    }

    private static BigDecimal lockAndRead(Connection connection, long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM account WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private static void setBalance(Connection connection, long id, BigDecimal value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE account SET balance = ? WHERE id = ?")) {
            ps.setBigDecimal(1, value);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}
