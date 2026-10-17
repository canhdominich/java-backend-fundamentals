package dev.canhdominich.junit.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    ProductRepository products;

    @Test
    void findsByExactName() {
        products.save(new Product("Book", 15_000));
        products.save(new Product("Pen", 5_000));

        assertThat(products.findByName("Book")).extracting(Product::getPrice).containsExactly(15_000L);
    }

    @Test
    void h2MatchesNamesCaseSensitively() {
        products.save(new Product("Book", 15_000));

        assertThat(products.findByName("BOOK")).isEmpty();
    }

    @Test
    void everyTestStartsWithAnEmptyTable() {
        assertThat(products.count()).isZero();
    }
}
