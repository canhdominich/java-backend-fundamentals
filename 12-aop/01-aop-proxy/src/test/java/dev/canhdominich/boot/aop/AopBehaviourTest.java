package dev.canhdominich.boot.aop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AopBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() {
        lines = AopDemo.run();
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
    void advisedBeansAreReplacedByProxies() {
        assertThat(lines).contains(
                "OrderService class: OrderService$$SpringCGLIB$$0 (CGLIB proxy, target OrderService)",
                "PriceCalculator class: PricingService$$SpringCGLIB$$0 (CGLIB proxy, target PricingService)");
    }

    @Test
    void adviceRunsInTheDocumentedOrderAroundAnAuditedMethod() {
        assertThat(block("place returned")).containsExactly(
                "place returned order-book",
                "    around: before place",
                "    before (audited): place",
                "    body: place book",
                "    body: private secret book",
                "    after returning: place -> order-book",
                "    after (finally): place",
                "    around: after place");
    }

    @Test
    void aspectOrderNestsOuterAspectsAroundInnerOnes() {
        assertThat(block("cancel returned")).containsExactly(
                "cancel returned cancelled book",
                "    outer aspect: before cancel",
                "    around: before cancel",
                "    body: cancel book",
                "    around: after cancel",
                "    outer aspect: after cancel");
    }

    @Test
    void callingThroughThisSkipsTheProxy() {
        assertThat(block("outer returned")).containsExactly(
                "outer returned order-book",
                "    around: before outer",
                "    body: outer book",
                "    body: place book",
                "    body: private secret book",
                "    around: after outer");
    }

    @Test
    void aFinalMethodIsNeitherAdvisedNorGivenTheTargetsState() {
        assertThat(block("receipt returned")).containsExactly("receipt returned nullbook", "    body: receipt book");
    }

    @Test
    void jdkProxiesAppearOnlyWhenClassProxiesAreTurnedOffAndAnInterfaceExists() {
        assertThat(lines).contains(
                "PriceCalculator class: $Proxy<n> (JDK dynamic proxy, target PricingService)",
                "OrderService class (no interface): OrderService$$SpringCGLIB$$0 (CGLIB proxy, target OrderService)",
                "getBean(PricingService.class): NoSuchBeanDefinitionException");
    }
}
