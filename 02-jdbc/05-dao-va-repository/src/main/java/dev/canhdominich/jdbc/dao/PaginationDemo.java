package dev.canhdominich.jdbc.dao;

import java.sql.SQLException;
import java.util.List;

public final class PaginationDemo {

    static final int ROWS = 500_000;
    static final int PAGE = 20;

    private PaginationDemo() {
    }

    public static void main(String[] args) throws SQLException {
        try (var dataSource = DataSources.create(DbConfig.fromEnvironment(), 2)) {
            Schema.reset(dataSource);
            Schema.seedProducts(dataSource, ROWS);
            JdbcProductRepository products = new JdbcProductRepository(dataSource);
            for (int offset : new int[]{0, 10_000, 100_000, 400_000, 499_000}) {
                System.out.printf("page starting at row %-7d  OFFSET: %-5s  keyset: %-5s%n", offset, millis(() -> products.findPageByOffset(offset, PAGE)), millis(() -> products.findPageAfter(offset, PAGE)));
            }
        }
    }

    static String millis(java.util.function.Supplier<List<Product>> query) {
        query.get();
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            List<Product> page = query.get();
            long elapsed = System.nanoTime() - start;
            if (page.size() != PAGE) {
                throw new IllegalStateException("unexpected page size " + page.size());
            }
            best = Math.min(best, elapsed);
        }
        return String.format("%.1fms", best / 1_000_000.0);
    }
}
