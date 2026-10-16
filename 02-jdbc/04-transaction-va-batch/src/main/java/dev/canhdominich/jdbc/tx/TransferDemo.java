package dev.canhdominich.jdbc.tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;

public final class TransferDemo {

    private TransferDemo() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        try (Connection connection = Db.open(config)) {
            Db.resetAccounts(connection);
            System.out.println("start                         : " + snapshot(connection));
            transfer(connection, 1, 2, new BigDecimal("30.00"));
            System.out.println("transfer 30 An->Binh          : " + snapshot(connection));
            try {
                transfer(connection, 1, 2, new BigDecimal("500.00"));
            } catch (SQLException e) {
                System.out.println("transfer 500 failed           : " + e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode());
            }
            System.out.println("after the failed transfer     : " + snapshot(connection));
            withoutTransaction(connection);
            System.out.println("autocommit, failure in step 2 : " + snapshot(connection));
        }
    }

    static void transfer(Connection connection, long from, long to, BigDecimal amount) throws SQLException {
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            update(connection, from, amount.negate());
            update(connection, to, amount);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    static void withoutTransaction(Connection connection) throws SQLException {
        try {
            update(connection, 2, new BigDecimal("-20.00"));
            update(connection, 1, new BigDecimal("-9999.00"));
        } catch (SQLException e) {
        }
    }

    static void savepointExample(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try {
            update(connection, 1, new BigDecimal("-10.00"));
            Savepoint sp = connection.setSavepoint("afterFirst");
            try {
                update(connection, 2, new BigDecimal("-9999.00"));
            } catch (SQLException e) {
                connection.rollback(sp);
            }
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
        }
    }

    static void update(Connection connection, long id, BigDecimal delta) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE account SET balance = balance + ? WHERE id = ?")) {
            ps.setBigDecimal(1, delta);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    static String snapshot(Connection connection) throws SQLException {
        return "An=" + Db.balance(connection, 1) + " Binh=" + Db.balance(connection, 2);
    }
}
