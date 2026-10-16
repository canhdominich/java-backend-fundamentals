package dev.canhdominich.jdbc.tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class IsolationDemo {

    record Observation(String level, BigDecimal whileOtherUncommitted, BigDecimal afterOtherCommitted) {
    }

    private IsolationDemo() {
    }

    public static void main(String[] args) throws SQLException {
        for (Observation o : observe(DbConfig.fromEnvironment())) {
            System.out.printf("%-17s while writer uncommitted=%-7s after writer committed=%s%n", o.level(), o.whileOtherUncommitted(), o.afterOtherCommitted());
        }
    }

    static List<Observation> observe(DbConfig config) throws SQLException {
        List<Observation> result = new ArrayList<>();
        int[] levels = {Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ};
        for (int level : levels) {
            try (Connection setup = Db.open(config)) {
                Db.resetAccounts(setup);
            }
            try (Connection writer = Db.open(config); Connection reader = Db.open(config)) {
                writer.setAutoCommit(false);
                reader.setAutoCommit(false);
                reader.setTransactionIsolation(level);
                TransferDemo.update(writer, 1, new BigDecimal("-60.00"));
                BigDecimal during = Db.balance(reader, 1);
                writer.commit();
                BigDecimal after = Db.balance(reader, 1);
                reader.commit();
                result.add(new Observation(name(level), during, after));
            }
        }
        return result;
    }

    static String name(int level) {
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            default -> "SERIALIZABLE";
        };
    }
}
