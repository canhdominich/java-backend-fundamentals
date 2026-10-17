package dev.canhdominich.junit.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssertionsTest {

    private static final List<String> REPORT = new ArrayList<>();

    @AfterAll
    static void print() {
        REPORT.forEach(System.out::println);
    }

    private static String oneLine(Throwable error) {
        return error.getMessage().strip().replaceAll("\\s*\\n\\s*", " ");
    }

    private static String firstLine(Throwable error) {
        return error.getMessage().lines().findFirst().orElse("");
    }

    private static void section(String title) {
        REPORT.add("-- " + title);
    }

    private static void line(String text) {
        REPORT.add("   " + text);
    }

    @Test
    @Order(1)
    void junitAndAssertjDescribeTheSameFailureDifferently() {
        section("the same wrong string, two messages");
        AssertionFailedError junit = assertThrows(AssertionFailedError.class, () -> assertEquals("hello world", "hello word"));
        AssertionError assertj = assertThrows(AssertionError.class, () -> assertThat("hello word").isEqualTo("hello world"));
        line("JUnit:   " + firstLine(junit));
        line("AssertJ: " + oneLine(assertj));
        assertThat(firstLine(junit)).isEqualTo("expected: <hello world> but was: <hello word>");
        assertThat(oneLine(assertj)).isEqualTo("expected: \"hello world\" but was: \"hello word\"");
    }

    @Test
    @Order(2)
    void collectionMessagesPointAtTheDifference() {
        section("two lists that differ in the last element");
        AssertionFailedError junit = assertThrows(AssertionFailedError.class, () -> assertEquals(List.of(1, 2, 3), List.of(1, 2, 4)));
        AssertionError assertj = assertThrows(AssertionError.class, () -> assertThat(List.of(1, 2, 4)).containsExactly(1, 2, 3));
        line("JUnit:   " + firstLine(junit));
        line("AssertJ: " + oneLine(assertj));
        AssertionError described = assertThrows(AssertionError.class, () -> assertThat(List.of(1, 2, 4)).as("cart lines").isEqualTo(List.of(1, 2, 3)));
        line("with .as(\"cart lines\"): " + oneLine(described));
        assertThat(firstLine(junit)).isEqualTo("expected: <[1, 2, 3]> but was: <[1, 2, 4]>");
        assertThat(oneLine(assertj)).isEqualTo("Expecting actual: [1, 2, 4] to contain exactly (and in same order): [1, 2, 3] but some elements were not found: [3] and others were not expected: [4]");
        assertThat(oneLine(described)).isEqualTo("[cart lines] expected: [1, 2, 3] but was: [1, 2, 4]");
    }

    @Test
    @Order(3)
    void swappedArgumentsGiveAMisleadingMessage() {
        section("expected first, actual second");
        int actual = 2 + 2;
        AssertionFailedError right = assertThrows(AssertionFailedError.class, () -> assertEquals(5, actual));
        AssertionFailedError swapped = assertThrows(AssertionFailedError.class, () -> assertEquals(actual, 5));
        line("assertEquals(5, actual):  " + firstLine(right));
        line("assertEquals(actual, 5):  " + firstLine(swapped));
        assertThat(firstLine(right)).isEqualTo("expected: <5> but was: <4>");
        assertThat(firstLine(swapped)).isEqualTo("expected: <4> but was: <5>");
    }

    @Test
    @Order(4)
    void floatingPointNeedsATolerance() {
        section("0.1 + 0.2");
        double sum = 0.1 + 0.2;
        AssertionFailedError exact = assertThrows(AssertionFailedError.class, () -> assertEquals(0.3, sum));
        line("exact comparison: " + firstLine(exact));
        assertEquals(0.3, sum, 1e-9);
        line("with a delta of 1e-9: passes");
        assertThat(firstLine(exact)).isEqualTo("expected: <0.3> but was: <0.30000000000000004>");
    }

    @Test
    @Order(5)
    void assertAllReportsEveryFailure() {
        section("assertAll against three checks that all fail");
        MultipleFailuresError grouped = assertThrows(MultipleFailuresError.class, () -> assertAll("customer",
                () -> assertEquals("An", "Binh"),
                () -> assertEquals(20, 21),
                () -> assertEquals("VN", "US")));
        line("assertAll reports " + grouped.getFailures().size() + " failures");
        int[] reached = {0};
        assertThrows(AssertionFailedError.class, () -> {
            reached[0]++;
            assertEquals("An", "Binh");
            reached[0]++;
            assertEquals(20, 21);
            reached[0]++;
            assertEquals("VN", "US");
        });
        line("three assertions in a row: stops after the first, " + reached[0] + " of 3 reached");
        assertThat(grouped.getFailures()).hasSize(3);
        assertThat(reached[0]).isEqualTo(1);
    }

    @Test
    @Order(6)
    void assertThrowsChecksTheTypeAndHandsBackTheException() {
        section("assertThrows");
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new DiscountPolicy().percent(0, false));
        line("returns the exception, message: " + thrown.getMessage());
        AssertionFailedError wrongType = assertThrows(AssertionFailedError.class,
                () -> assertThrows(IllegalStateException.class, () -> new DiscountPolicy().percent(0, false)));
        line("wrong type: " + firstLine(wrongType));
        assertThat(thrown).hasMessage("quantity must be positive: 0");
        assertThat(firstLine(wrongType)).startsWith("Unexpected exception type thrown");
    }
}
