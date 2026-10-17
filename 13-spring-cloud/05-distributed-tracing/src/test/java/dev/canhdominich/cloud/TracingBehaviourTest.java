package dev.canhdominich.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TracingBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = TracingDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void everyRequestGetsItsOwnTraceIdEvenWhenMostAreNotRecorded() {
        assertThat(line("100 requests")).endsWith("100 different trace ids, none missing: true");
        int recorded = Integer.parseInt(line("traces actually recorded").replaceAll("\\D+(\\d+) of 100", "$1"));
        assertThat(recorded).isBetween(1, 39);
    }

    @Test
    void oneTraceIdFlowsThroughAllThreeServices() {
        assertThat(line("services in the answer")).endsWith("[front, inventory, pricing]");
        assertThat(line("same trace id in all three")).endsWith("true");
        assertThat(line("span ids all different")).endsWith("true");
        assertThat(line("header received by inventory")).endsWith("true");
    }

    @Test
    void spansFormATreeWithClientAndServerSides() {
        assertThat(line("front parent")).endsWith("none");
        assertThat(line("spans recorded for the request")).endsWith("[front:CLIENT, front:SERVER, inventory:CLIENT, inventory:SERVER, pricing:SERVER]");
        assertThat(line("spans whose parent is missing")).endsWith("0");
    }

    @Test
    void logContextCarriesTheTraceId() {
        assertThat(line("trace id written to the log context")).endsWith("true");
    }

    @Test
    void outsideTraceIsContinuedAndNewThreadsLoseTheContext() {
        assertThat(line("a trace started outside")).endsWith("true");
        assertThat(line("front's parent")).endsWith("true");
        assertThat(line("request thread")).contains("other thread traceId=null");
    }

    @Test
    void zeroSamplingKeepsIdsButRecordsNothing() {
        assertThat(line("trace id still present")).endsWith("true");
        assertThat(line("spans recorded:")).endsWith("0");
    }
}
