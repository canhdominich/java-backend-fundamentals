package dev.canhdominich.junit.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TddStagesTest {

    private static final List<String> REPORT = new ArrayList<>();

    @AfterAll
    static void print() {
        REPORT.forEach(System.out::println);
    }

    private static List<ProbeRunner.Outcome> stage(String title, Class<?> implementation) {
        List<ProbeRunner.Outcome> outcomes = ProbeRunner.run(implementation);
        long failed = outcomes.stream().filter(o -> o.status().equals("FAILED")).count();
        REPORT.add("-- " + title);
        REPORT.add("   " + failed + " of " + outcomes.size() + " tests fail");
        outcomes.stream().filter(o -> o.status().equals("FAILED")).forEach(o -> REPORT.add("     " + o.test()));
        return outcomes;
    }

    private static long failed(List<ProbeRunner.Outcome> outcomes) {
        return outcomes.stream().filter(o -> o.status().equals("FAILED")).count();
    }

    @Test
    @Order(1)
    void redEveryNewBehaviourFailsFirst() {
        List<ProbeRunner.Outcome> outcomes = stage("1. Red: the contract against an implementation that always returns 0", Stages.Red.class);
        assertThat(outcomes).hasSize(11);
        assertThat(failed(outcomes)).isEqualTo(9);
    }

    @Test
    @Order(2)
    void greenMinimalCodeForTheFirstRule() {
        List<ProbeRunner.Outcome> outcomes = stage("2. Green for the first rule: 10 percent from quantity 10", Stages.FirstGreen.class);
        assertThat(failed(outcomes)).isEqualTo(7);
    }

    @Test
    @Order(3)
    void theTestsCatchAMissingCap() {
        List<ProbeRunner.Outcome> outcomes = stage("3. All rules written, the cap forgotten", Stages.MoreRules.class);
        assertThat(failed(outcomes)).isEqualTo(3);
        assertThat(outcomes.stream().filter(o -> o.status().equals("FAILED")).map(ProbeRunner.Outcome::test).collect(Collectors.toList()))
                .contains("quantity \"50\", member \"true\" gives \"25\" percent", "quantity \"100\", member \"true\" gives \"25\" percent");
    }

    @Test
    @Order(4)
    void refactoredImplementationKeepsEveryTestGreen() {
        List<ProbeRunner.Outcome> outcomes = stage("4. The real DiscountPolicy with the cap", Stages.Final.class);
        assertThat(failed(outcomes)).isZero();
        assertThat(outcomes).hasSize(11);
    }
}
