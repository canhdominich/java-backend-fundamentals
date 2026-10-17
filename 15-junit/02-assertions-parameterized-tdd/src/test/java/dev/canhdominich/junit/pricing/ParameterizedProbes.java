package dev.canhdominich.junit.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public final class ParameterizedProbes {

    static final List<String> LOG = new ArrayList<>();

    private ParameterizedProbes() {
    }

    public enum Tier {
        BASIC, MEMBER, VIP
    }

    public static class WithValueSource {

        @ParameterizedTest
        @ValueSource(ints = {2, 4, 6, 7, 8})
        void isEven(int number) {
            assertTrue(number % 2 == 0);
        }
    }

    public static class WithPlainLoop {

        @Test
        void isEven() {
            for (int number : new int[] {2, 4, 6, 7, 8}) {
                LOG.add("checked " + number);
                assertTrue(number % 2 == 0);
            }
        }
    }

    public static class WithCsvSource {

        @ParameterizedTest(name = "{0} plus {1} is {2}")
        @CsvSource({"2, 3, 5", "10, 20, 30", "-1, 1, 0"})
        void adds(int a, int b, int sum) {
            assertEquals(sum, a + b);
        }
    }

    public static class WithMethodSource {

        static Stream<Arguments> prices() {
            return Stream.of(Arguments.of("book", 15_000L), Arguments.of("pen", 5_000L));
        }

        @ParameterizedTest
        @MethodSource("prices")
        void priceIsPositive(String name, long price) {
            assertTrue(price > 0);
        }
    }

    public static class WithEnumSource {

        @ParameterizedTest
        @EnumSource(Tier.class)
        void everyTierIsKnown(Tier tier) {
            assertTrue(tier != null);
        }
    }

    public static class WithNullAndEmpty {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void blankIsRejected(String input) {
            assertTrue(input == null || input.isBlank());
        }
    }
}
