package dev.canhdominich.jdbc.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryTest {

    private HikariDataSource dataSource;
    private JdbcProductRepository products;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = DataSources.create(MySqlLab.appUser(), 3);
        Schema.reset(dataSource);
        products = new JdbcProductRepository(dataSource);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void insertFindAndDuplicateKeyTranslation() {
        Product saved = products.insert("KB-01", "Keyboard", new BigDecimal("49.90"), 10);
        assertThat(saved.id()).isPositive();
        assertThat(products.findBySku("KB-01")).contains(saved);
        assertThat(products.findBySku("missing")).isEqualTo(Optional.empty());
        assertThatThrownBy(() -> products.insert("KB-01", "Other", BigDecimal.ONE, 1))
                .isInstanceOf(DataAccessException.DuplicateKeyException.class)
                .hasMessage("insert product KB-01");
    }

    @Test
    void decreaseStockRefusesToGoNegative() {
        long id = products.insert("KB-01", "Keyboard", BigDecimal.TEN, 5).id();
        assertThat(products.decreaseStock(id, 3)).isTrue();
        assertThat(products.decreaseStock(id, 3)).isFalse();
        assertThat(products.findBySku("KB-01").orElseThrow().stock()).isEqualTo(2);
    }

    @Test
    void withoutTransactionTheStockChangeSticksWhenTheOrderInsertFails() {
        long id = products.insert("KB-01", "Keyboard", BigDecimal.TEN, 10).id();
        OrderService service = new OrderService(dataSource, products, new OrderRepository(dataSource));
        assertThatThrownBy(() -> service.placeOrderWithoutTransaction(null, id, 3)).isInstanceOf(DataAccessException.class);
        assertThat(products.findBySku("KB-01").orElseThrow().stock()).isEqualTo(7);
    }

    @Test
    void withTransactionTheStockChangeIsRolledBack() {
        long id = products.insert("KB-01", "Keyboard", BigDecimal.TEN, 10).id();
        OrderService service = new OrderService(dataSource, products, new OrderRepository(dataSource));
        assertThatThrownBy(() -> service.placeOrder(null, id, 3)).isInstanceOf(DataAccessException.class);
        assertThat(products.findBySku("KB-01").orElseThrow().stock()).isEqualTo(10);
        assertThat(TransactionContext.active()).isFalse();
    }

    @Test
    void successfulOrderCommitsAllThreeSteps() throws SQLException {
        long id = products.insert("KB-01", "Keyboard", BigDecimal.TEN, 10).id();
        OrderRepository orders = new OrderRepository(dataSource);
        OrderService service = new OrderService(dataSource, products, orders);
        long orderId = service.placeOrder("an@example.com", id, 2);
        assertThat(products.findBySku("KB-01").orElseThrow().stock()).isEqualTo(8);
        assertThat(orders.findAllWithJoin()).singleElement().satisfies(order -> {
            assertThat(order.id()).isEqualTo(orderId);
            assertThat(order.lines()).containsExactly(new OrderRepository.Line(id, 2));
        });
    }

    @Test
    void nestedTransactionJoinsTheOuterOne() {
        long id = products.insert("KB-01", "Keyboard", BigDecimal.TEN, 10).id();
        assertThatThrownBy(() -> TransactionContext.inTransaction(dataSource, () -> {
            products.decreaseStock(id, 4);
            TransactionContext.inTransaction(dataSource, () -> products.decreaseStock(id, 2));
            throw new IllegalStateException("fail after both");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(products.findBySku("KB-01").orElseThrow().stock()).isEqualTo(10);
    }

    @Test
    void oneQueryPerOrderIssuesNPlusOneStatementsAndJoinIssuesOne() throws SQLException {
        NPlusOneDemo.seedOrders(dataSource, 50);
        OrderRepository orders = new OrderRepository(dataSource);
        try (Connection probe = dataSource.getConnection()) {
            long before = QueryCounter.questions(probe);
            List<OrderRepository.Order> perOrder = orders.findAllOneQueryPerOrder();
            long middle = QueryCounter.questions(probe);
            List<OrderRepository.Order> joined = orders.findAllWithJoin();
            long after = QueryCounter.questions(probe);
            assertThat(middle - before - 1).isEqualTo(51);
            assertThat(after - middle - 1).isEqualTo(1);
            assertThat(joined).isEqualTo(perOrder);
        }
    }

    @Test
    void offsetAndKeysetPagesReturnTheSameRows() throws SQLException {
        Schema.seedProducts(dataSource, 1000);
        List<Product> byOffset = products.findPageByOffset(500, 20);
        List<Product> byKey = products.findPageAfter(byOffset.get(0).id() - 1, 20);
        assertThat(byKey).isEqualTo(byOffset);
    }

    @Test
    void serviceLogicRunsAgainstAFakeRepositoryWithoutADatabase() {
        ProductRepository fake = new ProductRepository() {
            private final List<Product> data = List.of(
                    new Product(1L, "A", "Charger", BigDecimal.ONE, 2),
                    new Product(2L, "B", "Adapter", BigDecimal.ONE, 50),
                    new Product(3L, "C", "Battery", BigDecimal.ONE, 1));

            public Product insert(String sku, String name, BigDecimal price, int stock) {
                throw new UnsupportedOperationException();
            }

            public Optional<Product> findBySku(String sku) {
                return data.stream().filter(p -> p.sku().equals(sku)).findFirst();
            }

            public List<Product> findAll() {
                return data;
            }

            public List<Product> findPageByOffset(int offset, int size) {
                throw new UnsupportedOperationException();
            }

            public List<Product> findPageAfter(long afterId, int size) {
                throw new UnsupportedOperationException();
            }

            public boolean decreaseStock(long productId, int quantity) {
                throw new UnsupportedOperationException();
            }
        };
        assertThat(new LowStockReport(fake).namesBelow(10)).containsExactly("Battery", "Charger");
    }
}
