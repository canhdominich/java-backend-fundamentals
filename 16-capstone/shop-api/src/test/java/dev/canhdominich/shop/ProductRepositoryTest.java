package dev.canhdominich.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.canhdominich.shop.product.Product;
import dev.canhdominich.shop.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        MySqlContainer.register(registry);
    }

    @Autowired
    ProductRepository products;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theMigrationsMatchTheEntities() {
        assertThat(products.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void decrementStockSucceedsOnlyWhileThereIsEnough() {
        Product saved = products.saveAndFlush(new Product("REPO-1", "Repo product", 100, 3));

        assertThat(products.decrementStock(saved.getId(), 2)).isEqualTo(1);
        assertThat(products.decrementStock(saved.getId(), 2)).isZero();
        assertThat(jdbc.queryForObject("select stock from products where id = ?", Integer.class, saved.getId())).isEqualTo(1);
    }

    @Test
    void theDatabaseItselfRefusesANegativeStock() {
        Product saved = products.saveAndFlush(new Product("REPO-2", "Repo product", 100, 1));

        assertThatThrownBy(() -> jdbc.update("update products set stock = -1 where id = ?", saved.getId()))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("Check constraint 'ck_products_stock' is violated");
    }

    @Test
    void aStaleUpdateIsRefusedBecauseTheStockMovedInTheMeantime() {
        Product loaded = products.saveAndFlush(new Product("REPO-4", "Repo product", 100, 5));
        products.decrementStock(loaded.getId(), 1);

        loaded.update("renamed", 200, 5);

        assertThatThrownBy(() -> products.saveAndFlush(loaded)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void theSkuIsUnique() {
        products.saveAndFlush(new Product("REPO-3", "first", 100, 1));

        assertThatThrownBy(() -> products.saveAndFlush(new Product("REPO-3", "second", 100, 1))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
