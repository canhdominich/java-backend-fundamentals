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
class ParameterizedMechanicsTest {

    private static final List<String> REPORT = new ArrayList<>();

    @AfterAll
    static void print() {
        REPORT.forEach(System.out::println);
    }

    private static List<ProbeRunner.Outcome> run(Class<?> probe) {
        ParameterizedProbes.LOG.clear();
        return ProbeRunner.run(probe);
    }

    private static String names(List<ProbeRunner.Outcome> outcomes) {
        return outcomes.stream().map(o -> o.test() + " " + o.status().toLowerCase()).collect(Collectors.joining("; "));
    }

    @Test
    @Order(1)
    void everyRowIsItsOwnTestAndABadRowDoesNotHideTheRest() {
        REPORT.add("-- one row fails: parameterized test against a loop inside one test");
        List<ProbeRunner.Outcome> rows = run(ParameterizedProbes.WithValueSource.class);
        REPORT.add("   @ValueSource {2, 4, 6, 7, 8}: " + names(rows));
        List<ProbeRunner.Outcome> loop = run(ParameterizedProbes.WithPlainLoop.class);
        REPORT.add("   plain loop in one test: " + loop.size() + " result, numbers checked " + ParameterizedProbes.LOG.stream().map(l -> l.replace("checked ", "")).collect(Collectors.toList()));
        assertThat(rows).extracting(ProbeRunner.Outcome::status).containsExactly("SUCCESSFUL", "SUCCESSFUL", "SUCCESSFUL", "FAILED", "SUCCESSFUL");
        assertThat(loop).hasSize(1);
        assertThat(ParameterizedProbes.LOG).containsExactly("checked 2", "checked 4", "checked 6", "checked 7");
    }

    @Test
    @Order(2)
    void csvSourceRowsBecomeNamedInvocations() {
        REPORT.add("-- @CsvSource with name = \"{0} plus {1} is {2}\"");
        List<ProbeRunner.Outcome> outcomes = run(ParameterizedProbes.WithCsvSource.class);
        outcomes.forEach(o -> REPORT.add("   " + o.test()));
        assertThat(outcomes).extracting(ProbeRunner.Outcome::test).containsExactly("\"2\" plus \"3\" is \"5\"", "\"10\" plus \"20\" is \"30\"", "\"-1\" plus \"1\" is \"0\"");
    }

    @Test
    @Order(3)
    void defaultNamesShowTheIndexAndTheArguments() {
        REPORT.add("-- default names");
        List<ProbeRunner.Outcome> method = run(ParameterizedProbes.WithMethodSource.class);
        List<ProbeRunner.Outcome> enums = run(ParameterizedProbes.WithEnumSource.class);
        List<ProbeRunner.Outcome> blank = run(ParameterizedProbes.WithNullAndEmpty.class);
        REPORT.add("   @MethodSource: " + method.stream().map(ProbeRunner.Outcome::test).collect(Collectors.toList()));
        REPORT.add("   @EnumSource: " + enums.stream().map(ProbeRunner.Outcome::test).collect(Collectors.toList()));
        REPORT.add("   @NullAndEmptySource plus one blank string: " + blank.stream().map(ProbeRunner.Outcome::test).collect(Collectors.toList()));
        assertThat(method).extracting(ProbeRunner.Outcome::test).containsExactly("[1] \"book\", 15000", "[2] \"pen\", 5000");
        assertThat(enums).extracting(ProbeRunner.Outcome::test).containsExactly("[1] BASIC", "[2] MEMBER", "[3] VIP");
        assertThat(blank).extracting(ProbeRunner.Outcome::test).containsExactly("[1] null", "[2] \"\"", "[3] \" \"");
    }
}
