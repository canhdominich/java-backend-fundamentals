package dev.canhdominich.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FeignBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = FeignDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void interfaceBecomesAWorkingClient() {
        assertThat(line("client bean is a generated proxy")).endsWith("true");
        assertThat(line("pay(7) returned")).endsWith("paid-7");
    }

    @Test
    void httpErrorsBecomeTypedExceptions() {
        assertThat(line("pay(404) threw")).endsWith("NotFound, status 404");
        assertThat(line("payment down")).endsWith("InternalServerError, status 500");
    }

    @Test
    void readTimeoutCutsASlowCall() {
        assertThat(line("default timeouts")).endsWith("paid-7 in 2000 ms");
        assertThat(line("read-timeout 500 ms")).contains("RetryableException").endsWith("in 500 ms");
    }

    @Test
    void fallbackNeedsTheCircuitBreaker() {
        assertThat(line("fallback declared, breaker off")).endsWith("InternalServerError, status 500");
        assertThat(line("payment answers in 2 s")).contains("pending-7").endsWith("in 1000 ms");
    }

    @Test
    void openCircuitStopsCallsReachingPayment() {
        assertThat(line("failing call 3")).endsWith("=CLOSED]");
        assertThat(line("failing call 4")).endsWith("=OPEN]");
        assertThat(line("payment hits so far")).endsWith("4");
        assertThat(line("payment hits after calls 5 and 6")).endsWith("4");
        assertThat(line("call 5")).contains("under 50 ms");
        assertThat(line("after 3.5 s")).contains("paid-7").endsWith("=HALF_OPEN]");
        assertThat(line("next call")).endsWith("=CLOSED]");
    }
}
