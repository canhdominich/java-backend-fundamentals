package dev.canhdominich.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiscoveryBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = DiscoveryDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void registryStartsEmpty() {
        assertThat(line("catalog instances in the registry")).endsWith("[]");
    }

    @Test
    void bothInstancesRegisterAndShopSeesThem() {
        assertThat(line("shop sees 2 instances")).endsWith("true");
        assertThat(line("instances listed by the registry")).endsWith("[catalog-A, catalog-B]");
    }

    @Test
    void loadBalancerAlternatesBetweenInstances() {
        assertThat(line("20 calls")).endsWith("{catalog-A=10, catalog-B=10}");
    }

    @Test
    void gracefulShutdownRemovesTheInstanceQuickly() {
        assertThat(line("registry lists only catalog-B within 15 s")).endsWith("true");
        assertThat(line("shop sees 1 instance")).endsWith("true");
        assertThat(line("10 calls afterwards")).endsWith("{catalog-B=10}");
    }

    @Test
    void crashCausesFailuresUntilTheLeaseExpires() {
        assertThat(line("some calls failed")).endsWith("true");
        assertThat(line("failures stopped by themselves")).endsWith("true");
        assertThat(line("registry lists only catalog-B at the end")).endsWith("[catalog-B]");
    }
}
