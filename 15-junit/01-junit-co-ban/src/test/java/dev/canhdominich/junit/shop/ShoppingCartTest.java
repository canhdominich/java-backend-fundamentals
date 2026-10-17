package dev.canhdominich.junit.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShoppingCart")
class ShoppingCartTest {

    private ShoppingCart cart;

    @BeforeEach
    void createCart() {
        cart = new ShoppingCart();
    }

    @Test
    @DisplayName("starts empty with a total of zero")
    void startsEmpty() {
        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.total()).isZero();
    }

    @Test
    @DisplayName("adds quantity times unit price")
    void totalsQuantityTimesPrice() {
        cart.add("BOOK", 2, 15_000);
        cart.add("PEN", 3, 5_000);
        assertThat(cart.total()).isEqualTo(45_000);
        assertThat(cart.itemCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("merges two additions of the same sku")
    void mergesSameSku() {
        cart.add("BOOK", 1, 15_000);
        cart.add("BOOK", 2, 15_000);
        assertThat(cart.itemCount()).isEqualTo(3);
        assertThat(cart.total()).isEqualTo(45_000);
    }

    @Test
    @DisplayName("rejects a quantity that is not positive")
    void rejectsBadQuantity() {
        assertThatThrownBy(() -> cart.add("BOOK", 0, 15_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be positive: 0");
    }

    @Nested
    @DisplayName("after clear()")
    class AfterClear {

        @BeforeEach
        void fillThenClear() {
            cart.add("BOOK", 2, 15_000);
            cart.clear();
        }

        @Test
        @DisplayName("is empty again")
        void isEmpty() {
            assertThat(cart.isEmpty()).isTrue();
            assertThat(cart.total()).isZero();
        }
    }
}
