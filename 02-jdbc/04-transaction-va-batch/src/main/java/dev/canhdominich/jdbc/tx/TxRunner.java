package dev.canhdominich.jdbc.tx;

import java.sql.Connection;
import java.sql.SQLException;

final class TxRunner {

    interface ConnectionSource {
        Connection open() throws SQLException;
    }

    interface Work {
        void run(Connection connection) throws SQLException;
    }

    private TxRunner() {
    }

    static void inTransaction(ConnectionSource source, Work work, int maxAttempts) throws SQLException {
        SQLException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try (Connection connection = source.open()) {
                connection.setAutoCommit(false);
                try {
                    work.run(connection);
                    connection.commit();
                    return;
                } catch (SQLException e) {
                    connection.rollback();
                    last = e;
                    if (!"40001".equals(e.getSQLState())) {
                        throw e;
                    }
                }
            }
        }
        throw last;
    }
}
