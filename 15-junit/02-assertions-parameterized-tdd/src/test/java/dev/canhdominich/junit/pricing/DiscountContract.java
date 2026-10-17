package dev.canhdominich.junit.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

abstract class DiscountContract {

    abstract DiscountRules rules();

    @ParameterizedTest(name = "quantity {0}, member {1} gives {2} percent")
    @CsvSource({
        "1,  false, 0",
        "9,  false, 0",
        "10, false, 10",
        "49, false, 10",
        "50, false, 20",
        "1,  true,  8",
        "10, true,  18",
        "50, true,  25",
        "100, true, 25"
    })
    void discountPercent(int quantity, boolean member, int expected) {
        assertThat(rules().percent(quantity, member)).isEqualTo(expected);
    }

    @Test
    void rejectsAQuantityThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> rules().percent(0, false));
    }

    @Test
    void totalAppliesTheDiscount() {
        assertThat(new PriceCalculator(rules()).total(10_000, 50, true)).isEqualTo(375_000);
    }
}
