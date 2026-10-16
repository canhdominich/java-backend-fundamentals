package dev.canhdominich.jdbc.tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class DeadlockDemo {

    private DeadlockDemo() {
    }

    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.fromEnvironment();
        for (String line : crossUpdate(config)) {
            System.out.println(line);
        }
        System.out.println("lock wait timeout: " + lockWaitTimeout(config));
        System.out.println("retry helper     : " + retryHelper(config));
    }

    static java.util.List<String> crossUpdate(DbConfig config) throws Exception {
        try (Connection setup = Db.open(config)) {
            Db.resetAccounts(setup);
        }
        CountDownLatch bothHoldFirstLock = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<String> one = pool.submit(() -> worker(config, 1, 2, bothHoldFirstLock));
        Future<String> two = pool.submit(() -> worker(config, 2, 1, bothHoldFirstLock));
        java.util.List<String> lines = java.util.List.of("worker 1->2: " + one.get(), "worker 2->1: " + two.get());
        pool.shutdown();
        return lines;
    }

    private static String worker(DbConfig config, long first, long second, CountDownLatch latch) throws Exception {
        try (Connection connection = Db.open(config)) {
            connection.setAutoCommit(false);
            try {
                TransferDemo.update(connection, first, new BigDecimal("-1.00"));
                latch.countDown();
                latch.await();
                TransferDemo.update(connection, second, new BigDecimal("1.00"));
                connection.commit();
                return "committed";
            } catch (SQLException e) {
                connection.rollback();
                return e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode();
            }
        }
    }

    static String lockWaitTimeout(DbConfig config) throws SQLException {
        try (Connection setup = Db.open(config)) {
            Db.resetAccounts(setup);
        }
        try (Connection holder = Db.open(config); Connection waiter = Db.open(config)) {
            holder.setAutoCommit(false);
            TransferDemo.update(holder, 1, new BigDecimal("-1.00"));
            try (var statement = waiter.createStatement()) {
                statement.execute("SET SESSION innodb_lock_wait_timeout = 2");
            }
            long start = System.nanoTime();
            try {
                TransferDemo.update(waiter, 1, new BigDecimal("-1.00"));
                return "no timeout";
            } catch (SQLException e) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                holder.rollback();
                return e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " after ~" + (ms / 100 * 100) + " ms";
            }
        }
    }

    static String retryHelper(DbConfig config) throws Exception {
        try (Connection setup = Db.open(config)) {
            Db.resetAccounts(setup);
        }
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        int[] attempts = new int[2];
        Future<?> one = pool.submit(() -> run(config, 1, 2, latch, attempts, 0));
        Future<?> two = pool.submit(() -> run(config, 2, 1, latch, attempts, 1));
        one.get();
        two.get();
        pool.shutdown();
        try (Connection check = Db.open(config)) {
            return "attempts " + attempts[0] + " and " + attempts[1] + ", final " + TransferDemo.snapshot(check);
        }
    }

    private static void run(DbConfig config, long first, long second, CountDownLatch latch, int[] attempts, int slot) {
        try {
            TxRunner.inTransaction(() -> Db.open(config), connection -> {
                attempts[slot]++;
                TransferDemo.update(connection, first, new BigDecimal("-1.00"));
                if (attempts[slot] == 1) {
                    latch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                TransferDemo.update(connection, second, new BigDecimal("1.00"));
            }, 3);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
