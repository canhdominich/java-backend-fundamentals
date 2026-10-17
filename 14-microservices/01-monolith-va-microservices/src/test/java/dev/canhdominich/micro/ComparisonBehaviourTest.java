package dev.canhdominich.micro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ComparisonBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = ComparisonDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    private static double[] numbers(String text) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?) (?:MB|s|ms)").matcher(text);
        List<Double> values = new java.util.ArrayList<>();
        while (matcher.find()) {
            values.add(Double.parseDouble(matcher.group(1)));
        }
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    @Test
    void twoServicesCostMoreMemoryThanOneApplication() {
        double[] mb = numbers(line("memory after warm-up"));
        assertThat(mb[1]).isGreaterThan(mb[0]);
    }

    @Test
    void startingTwoServicesTakesLongerThanOne() {
        double[] seconds = numbers(line("time until ready"));
        assertThat(seconds[1]).isGreaterThan(seconds[0]);
    }

    @Test
    void anExtraNetworkHopSlowsEachCall() {
        assertThat(line("one extra network hop")).endsWith("true");
    }

    @Test
    void monolithRollsBackBothChanges() {
        assertThat(line("monolith: status")).isEqualTo("monolith: status 500, stock change 0, orders change 0");
    }

    @Test
    void microservicesLeaveTheStockReservedWithoutAnOrder() {
        assertThat(line("microservices: status")).isEqualTo("microservices: status 500, stock change -5, orders change 0");
    }

    @Test
    void stoppedInventoryBreaksOrdersButNotTheOrderServicesOwnData() {
        assertThat(line("order service, placing an order")).endsWith("status 500");
        assertThat(line("order service, reading its own data")).endsWith("status 200");
    }
}
