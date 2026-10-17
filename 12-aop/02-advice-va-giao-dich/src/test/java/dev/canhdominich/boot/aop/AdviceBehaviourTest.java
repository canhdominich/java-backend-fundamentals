package dev.canhdominich.boot.aop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AdviceBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() {
        lines = AdviceDemo.run();
    }

    private static List<String> block(String prefix) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                start = i;
                break;
            }
        }
        assertThat(start).as(prefix).isGreaterThanOrEqualTo(0);
        int end = start + 1;
        while (end < lines.size() && lines.get(end).startsWith("    ")) {
            end++;
        }
        return lines.subList(start, end);
    }

    @Test
    void afterReturningRunsOnSuccessAfterThrowingOnFailureAndAfterAlways() {
        assertThat(block("charge(100)")).containsExactly("charge(100) returned charged 100",
                "    around: before", "    before", "    body: charge 100", "    after returning: charged 100", "    after (finally)", "    around: after");
        assertThat(block("charge(0)")).containsExactly("charge(0) threw IllegalStateException: amount must be positive",
                "    around: before", "    before", "    body: charge 0", "    after throwing: amount must be positive", "    after (finally)", "    around: after");
    }

    @Test
    void aroundCanChangeTheResultAndSkipTheTarget() {
        assertThat(lines).contains("changes the result: RECEIPT-12345", "skips the call: priceOf twice gave 4000 and 4000, target ran 1 time(s)");
    }

    @Test
    void transactionsAreAdviceOnTheSameProxy() {
        assertThat(lines.stream().filter(l -> l.startsWith("advice objects on the FlakyWriter proxy")).findFirst().orElseThrow()).contains("TransactionInterceptor");
    }

    @Test
    void aRetryOutsideTheTransactionStartsAFreshTransactionEachAttempt() {
        assertThat(block("saveWithRetryOutsideTransaction")).containsExactly("saveWithRetryOutsideTransaction returned saved",
                "    retry outside the transaction: attempt 1, transaction active = false",
                "    retry outside the transaction: attempt 1 failed",
                "    retry outside the transaction: attempt 2, transaction active = false",
                "    retry outside the transaction: attempt 2 failed",
                "    retry outside the transaction: attempt 3, transaction active = false");
        assertThat(lines).contains("rows saved: 1");
    }

    @Test
    void aRetryInsideTheTransactionKeepsTheWritesOfFailedAttempts() {
        assertThat(block("saveWithRetryInsideTransaction")).contains("    retry inside the transaction: attempt 1, transaction active = true", "    retry inside the transaction: attempt 3, transaction active = true");
        assertThat(lines.get(lines.size() - 1)).isEqualTo("rows saved: 3");
    }
}
