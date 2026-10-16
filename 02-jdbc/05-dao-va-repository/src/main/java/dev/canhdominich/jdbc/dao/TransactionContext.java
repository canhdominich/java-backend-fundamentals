package dev.canhdominich.jdbc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.function.Supplier;
import javax.sql.DataSource;

final class TransactionContext {

    interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    private static final ThreadLocal<Connection> BOUND = new ThreadLocal<>();

    private TransactionContext() {
    }

    static boolean active() {
        return BOUND.get() != null;
    }

    static <T> T withConnection(DataSource dataSource, String description, SqlWork<T> work) {
        Connection bound = BOUND.get();
        try {
            if (bound != null) {
                return work.apply(bound);
            }
            try (Connection connection = dataSource.getConnection()) {
                return work.apply(connection);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == 1062) {
                throw new DataAccessException.DuplicateKeyException(description, e);
            }
            throw new DataAccessException(description, e);
        } catch (SQLException e) {
            throw new DataAccessException(description, e);
        }
    }

    static <T> T inTransaction(DataSource dataSource, Supplier<T> work) {
        if (BOUND.get() != null) {
            return work.get();
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            BOUND.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                BOUND.remove();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("transaction", e);
        }
    }
}
