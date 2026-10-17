package dev.canhdominich.micro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SagaBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = SagaDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void withoutCompensationTheStockStaysReserved() {
        assertThat(line("without compensation")).isEqualTo("without compensation: status 500, stock change -5, orders 0");
    }

    @Test
    void compensationReleasesTheReservation() {
        assertThat(line("with compensation")).isEqualTo("with compensation: status 500, stock change 0, orders 0");
    }

    @Test
    void aRetryWithoutAKeyReservesTwice() {
        assertThat(line("retry without a key")).isEqualTo("retry without a key: status 200, stock change -10, orders 1");
    }

    @Test
    void aKeyMakesTheRetryHarmless() {
        assertThat(line("retry with a key")).isEqualTo("retry with a key: status 200, stock change -5, orders 1");
    }

    @Test
    void anUnknownOutcomeIsCompensatedByKey() {
        assertThat(line("keyed reserve, unknown outcome")).isEqualTo("keyed reserve, unknown outcome, compensated: status 500, stock change 0, orders 0");
    }

    @Test
    void aFailedCompensationIsRetriedLater() {
        assertThat(line("release is down")).isEqualTo("release is down: status 500, stock change -5, orders 0, pending compensations 1");
        assertThat(line("compensations delivered")).endsWith(": 1");
        assertThat(line("after the retry")).isEqualTo("after the retry: stock change 0, pending compensations 0");
    }

    @Test
    void releasingTwiceRestoresTheStockOnce() {
        assertThat(line("the order service already released it")).isEqualTo("the order service already released it; releasing by hand: already released, stock change 0");
        assertThat(line("releasing a key that never reserved")).endsWith("nothing to release");
    }
}
