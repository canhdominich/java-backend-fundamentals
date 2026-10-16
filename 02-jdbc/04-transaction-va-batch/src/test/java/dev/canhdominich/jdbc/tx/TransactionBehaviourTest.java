package dev.canhdominich.jdbc.tx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionBehaviourTest {

    private final DbConfig app = MySqlLab.appUser();

    @BeforeEach
    void resetAccounts() throws SQLException {
        try (Connection connection = Db.open(app)) {
            Db.resetAccounts(connection);
        }
    }

    @Test
    void transferCommitsBothStepsOrNeither() throws SQLException {
        try (Connection connection = Db.open(app)) {
            TransferDemo.transfer(connection, 1, 2, new BigDecimal("30.00"));
            assertThat(TransferDemo.snapshot(connection)).isEqualTo("An=70.00 Binh=80.00");
            assertThatThrownBy(() -> TransferDemo.transfer(connection, 1, 2, new BigDecimal("500.00")))
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> assertThat(((SQLException) e).getErrorCode()).isEqualTo(3819));
            assertThat(TransferDemo.snapshot(connection)).isEqualTo("An=70.00 Binh=80.00");
            assertThat(connection.getAutoCommit()).isTrue();
        }
    }

    @Test
    void withoutTransactionTheFirstStepSticksWhenTheSecondFails() throws SQLException {
        try (Connection connection = Db.open(app)) {
            TransferDemo.withoutTransaction(connection);
            assertThat(TransferDemo.snapshot(connection)).isEqualTo("An=100.00 Binh=30.00");
        }
    }

    @Test
    void closingWithoutCommitDiscardsTheChanges() throws SQLException {
        try (Connection connection = Db.open(app)) {
            connection.setAutoCommit(false);
            TransferDemo.update(connection, 1, new BigDecimal("-40.00"));
        }
        try (Connection check = Db.open(app)) {
            assertThat(Db.balance(check, 1)).isEqualByComparingTo("100.00");
        }
    }

    @Test
    void savepointKeepsTheWorkBeforeIt() throws SQLException {
        try (Connection connection = Db.open(app)) {
            TransferDemo.savepointExample(connection);
            assertThat(TransferDemo.snapshot(connection)).isEqualTo("An=90.00 Binh=50.00");
        }
    }

    @Test
    void isolationLevelsDecideWhatTheReaderSees() throws SQLException {
        List<IsolationDemo.Observation> observed = IsolationDemo.observe(app);
        assertThat(observed).extracting(IsolationDemo.Observation::level)
                .containsExactly("READ_UNCOMMITTED", "READ_COMMITTED", "REPEATABLE_READ");
        assertThat(observed.get(0).whileOtherUncommitted()).isEqualByComparingTo("40.00");
        assertThat(observed.get(1).whileOtherUncommitted()).isEqualByComparingTo("100.00");
        assertThat(observed.get(1).afterOtherCommitted()).isEqualByComparingTo("40.00");
        assertThat(observed.get(2).afterOtherCommitted()).isEqualByComparingTo("100.00");
    }

    @Test
    void lostUpdateHappensOnlyWhenTheApplicationWritesAStaleAbsoluteValue() throws Exception {
        assertThat(LostUpdateDemo.readThenWrite(app)).isEqualByComparingTo("80.00");
        assertThat(LostUpdateDemo.atomic(app)).isEqualByComparingTo("70.00");
        long[] waited = new long[1];
        assertThat(LostUpdateDemo.forUpdate(app, waited)).isEqualByComparingTo("70.00");
        assertThat(waited[0]).isGreaterThanOrEqualTo(400);
        assertThat(LostUpdateDemo.optimistic(app)).isEqualTo("first writer rows=1, second writer (stale version) rows=0");
    }

    @Test
    void crossingUpdatesDeadlockAndInnoDbPicksOneVictim() throws Exception {
        List<String> outcome = DeadlockDemo.crossUpdate(app);
        long victims = outcome.stream().filter(line -> line.contains("SQLState=40001 errorCode=1213")).count();
        long winners = outcome.stream().filter(line -> line.endsWith("committed")).count();
        assertThat(victims).isEqualTo(1);
        assertThat(winners).isEqualTo(1);
    }

    @Test
    void lockWaitTimeoutSharesSqlStateWithDeadlock() throws SQLException {
        String result = DeadlockDemo.lockWaitTimeout(app);
        assertThat(result).contains("SQLState=40001").contains("errorCode=1205");
    }

    @Test
    void retryHelperResolvesTheDeadlock() throws Exception {
        String result = DeadlockDemo.retryHelper(app);
        assertThat(result).endsWith("final An=100.00 Binh=50.00");
    }

    @Test
    void batchFailureLeavesPartialRowsByDefaultAndNoneWhenRewritten() throws SQLException {
        try (Connection connection = Db.open(app)) {
            assertThat(BatchErrors.describe(connection)).contains("counts [1, 1, -3, 1, 1]").contains("rows stored 4");
        }
        try (Connection connection = Db.open(app, "rewriteBatchedStatements=true")) {
            assertThat(BatchErrors.describe(connection)).contains("counts [-3, -3, -3, -3, -3]").contains("rows stored 0");
        }
    }

    @Test
    void rewrittenBatchBeatsAutocommitPerRow() throws SQLException {
        long perRow;
        long rewritten;
        try (Connection connection = Db.open(app)) {
            BatchBenchmark.reset(connection);
            long start = System.nanoTime();
            BatchBenchmark.insert(connection, BatchBenchmark.Mode.AUTOCOMMIT_EACH, 200);
            perRow = System.nanoTime() - start;
        }
        try (Connection connection = Db.open(app, "rewriteBatchedStatements=true")) {
            BatchBenchmark.reset(connection);
            long start = System.nanoTime();
            BatchBenchmark.insert(connection, BatchBenchmark.Mode.BATCH, 200);
            rewritten = System.nanoTime() - start;
        }
        assertThat(rewritten).isLessThan(perRow);
    }
}
