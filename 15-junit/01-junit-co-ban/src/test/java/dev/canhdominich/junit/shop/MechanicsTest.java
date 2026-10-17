package dev.canhdominich.junit.shop;

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
class MechanicsTest {

    private static final List<String> REPORT = new ArrayList<>();

    @AfterAll
    static void print() {
        REPORT.forEach(System.out::println);
    }

    private static void section(String title) {
        REPORT.add("-- " + title);
    }

    private static void line(String text) {
        REPORT.add("   " + text);
    }

    @Test
    @Order(1)
    void everyTestGetsItsOwnInstanceByDefault() {
        section("default lifecycle: a new instance for every test method");
        ProbeRunner.run(Probes.PerMethod.class);
        Probes.LOG.forEach(MechanicsTest::line);
        assertThat(Probes.LOG).filteredOn(entry -> entry.startsWith("constructor")).containsExactly("constructor instance A", "constructor instance B");
        assertThat(Probes.LOG.get(0)).isEqualTo("beforeAll");
        assertThat(Probes.LOG.get(Probes.LOG.size() - 1)).isEqualTo("afterAll");
    }

    @Test
    @Order(2)
    void perClassLifecycleKeepsOneInstance() {
        section("PER_CLASS lifecycle: one instance for the whole class");
        ProbeRunner.run(Probes.PerClass.class);
        Probes.LOG.forEach(MechanicsTest::line);
        assertThat(Probes.LOG).filteredOn(entry -> entry.startsWith("constructor")).containsExactly("constructor instance A");
        assertThat(Probes.LOG).contains("beforeAll on instance A");
    }

    @Test
    @Order(3)
    void outcomesAreMoreThanPassAndFail() {
        section("six tests: four statuses, three kinds of failure");
        List<ProbeRunner.Outcome> outcomes = ProbeRunner.run(Probes.Outcomes.class).stream()
                .sorted((a, b) -> a.test().compareTo(b.test())).toList();
        outcomes.forEach(outcome -> line(String.format("%-22s %-10s %s", outcome.test(), outcome.status(), outcome.cause())));
        assertThat(outcomes).extracting(ProbeRunner.Outcome::status).containsExactly("ABORTED", "FAILED", "SKIPPED", "SUCCESSFUL", "FAILED", "FAILED");
        assertThat(outcomes).extracting(ProbeRunner.Outcome::cause).containsExactly("TestAbortedException", "AssertionFailedError", "not ready", "", "TimeoutException", "NullPointerException");
    }

    @Test
    @Order(4)
    void defaultOrderIsNeitherDeclarationNorAlphabetical() {
        section("default method order (declared zulu, alpha, mike, bravo)");
        ProbeRunner.run(Probes.DefaultOrder.class);
        line(String.join(", ", Probes.LOG));
        assertThat(Probes.LOG).containsExactlyInAnyOrder("zulu", "alpha", "mike", "bravo");
        assertThat(Probes.LOG).isNotEqualTo(List.of("zulu", "alpha", "mike", "bravo"));
        assertThat(Probes.LOG).isNotEqualTo(List.of("alpha", "bravo", "mike", "zulu"));
    }

    @Test
    @Order(5)
    void orderAnnotationMakesTheOrderExplicit() {
        section("with @TestMethodOrder and @Order");
        ProbeRunner.run(Probes.ExplicitOrder.class);
        line(String.join(", ", Probes.LOG));
        assertThat(Probes.LOG).containsExactly("alpha", "mike", "zulu");
    }

    @Test
    @Order(6)
    void tagsSelectTests() {
        section("tags");
        int all = ProbeRunner.run(Probes.Tagged.class).size();
        List<ProbeRunner.Outcome> smoke = ProbeRunner.run(Probes.Tagged.class, "smoke");
        line("without a filter: " + all + " tests; only tag smoke: " + smoke.stream().map(ProbeRunner.Outcome::test).sorted().collect(Collectors.toList()));
        assertThat(all).isEqualTo(4);
        assertThat(smoke).extracting(ProbeRunner.Outcome::test).containsExactlyInAnyOrder("smokeOne()", "smokeTwo()");
    }

    @Test
    @Order(7)
    void repeatedTestRunsTheMethodSeveralTimes() {
        section("@RepeatedTest(3)");
        List<ProbeRunner.Outcome> outcomes = ProbeRunner.run(Probes.Repeated.class);
        line(outcomes.stream().map(ProbeRunner.Outcome::test).collect(Collectors.toList()).toString());
        assertThat(outcomes).extracting(ProbeRunner.Outcome::test).containsExactly("repetition 1 of 3", "repetition 2 of 3", "repetition 3 of 3");
    }
}
