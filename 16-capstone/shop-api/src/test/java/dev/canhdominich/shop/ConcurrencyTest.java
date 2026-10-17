package dev.canhdominich.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ConcurrencyTest {

    private static final int BUYERS = 20;
    private static final int STOCK = 10;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        MySqlContainer.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private static final List<String> REPORT = new ArrayList<>();

    @org.junit.jupiter.api.AfterAll
    static void print() {
        REPORT.stream().sorted().forEach(System.out::println);
    }

    ApiClient api;

    @BeforeEach
    void createClient() {
        api = new ApiClient(mvc);
    }

    private <T> List<T> runTogether(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await();
        go.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        pool.shutdown();
        return results;
    }

    @Test
    void twentyBuyersOfTenItemsEndWithExactlyTenOrders() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "RACE-1", 100_000, STOCK);
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            tokens.add(api.registerAndLogin(api.uniqueEmail()));
        }
        AtomicInteger next = new AtomicInteger();
        List<Integer> statuses = runTogether(BUYERS, () -> mvc.perform(api.order(tokens.get(next.getAndIncrement()), null, productId, 1)).andReturn().getResponse().getStatus());

        REPORT.add("1 atomic update: " + statuses.stream().filter(status -> status == 201).count() + " orders created, " + statuses.stream().filter(status -> status == 409).count() + " rejected with 409, stock left " + api.stockOf(productId));
        assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(STOCK);
        assertThat(statuses.stream().filter(status -> status == 409).count()).isEqualTo(BUYERS - STOCK);
        assertThat(api.stockOf(productId)).isZero();
        Integer sold = jdbc.queryForObject("select coalesce(sum(quantity), 0) from order_lines where product_id = ?", Integer.class, productId);
        assertThat(sold).isEqualTo(STOCK);
    }

    @Test
    void aReadThenWriteImplementationOversellsUnderTheSameLoad() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "RACE-2", 100_000, STOCK);
        AtomicInteger accepted = new AtomicInteger();
        runTogether(BUYERS, () -> {
            Integer stock = jdbc.queryForObject("select stock from products where id = ?", Integer.class, productId);
            Thread.sleep(50);
            if (stock != null && stock >= 1) {
                jdbc.update("update products set stock = ? where id = ?", stock - 1, productId);
                accepted.incrementAndGet();
            }
            return stock;
        });

        REPORT.add("2 read then write: " + accepted.get() + " buyers accepted for a stock of " + STOCK + ", stock column says " + api.stockOf(productId));
        assertThat(accepted.get()).isGreaterThan(STOCK);
        assertThat(api.stockOf(productId)).isGreaterThan(0);
    }

    @Test
    void ordersThatListTheSameProductsInOppositeOrderNeverDeadlock() throws Exception {
        String admin = api.adminToken();
        long first = api.createProduct(admin, "LOCK-A", 100_000, 500);
        long second = api.createProduct(admin, "LOCK-B", 100_000, 500);
        String customer = api.registerAndLogin(api.uniqueEmail());
        AtomicInteger next = new AtomicInteger();
        List<Integer> statuses = runTogether(40, () -> {
            boolean forward = next.getAndIncrement() % 2 == 0;
            long one = forward ? first : second;
            long two = forward ? second : first;
            String body = "{\"lines\":[{\"productId\":" + one + ",\"quantity\":1},{\"productId\":" + two + ",\"quantity\":1}]}";
            return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/orders").header("Authorization", "Bearer " + customer)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
        });

        REPORT.add("4 opposite line order, 40 requests: " + statuses.stream().filter(status -> status == 201).count() + " created, " + statuses.stream().filter(status -> status >= 500).count() + " server errors");
        assertThat(statuses).allMatch(status -> status == 201);
        assertThat(api.stockOf(first)).isEqualTo(460);
        assertThat(api.stockOf(second)).isEqualTo(460);
    }

    @Test
    void twoSimultaneousRequestsWithTheSameKeyCreateOneOrder() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "RACE-3", 100_000, 5);
        String customer = api.registerAndLogin(api.uniqueEmail());
        List<Integer> statuses = runTogether(6, () -> mvc.perform(api.order(customer, "same-key", productId, 1)).andReturn().getResponse().getStatus());

        REPORT.add("3 same key, 6 requests: " + statuses.stream().filter(status -> status == 201).count() + " created, " + statuses.stream().filter(status -> status == 200).count() + " replayed, stock left " + api.stockOf(productId));
        assertThat(statuses).allMatch(status -> status == 200 || status == 201);
        assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(1);
        assertThat(api.stockOf(productId)).isEqualTo(4);
    }
}
