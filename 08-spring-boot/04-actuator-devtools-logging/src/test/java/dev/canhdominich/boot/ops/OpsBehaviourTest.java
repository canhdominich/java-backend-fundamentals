package dev.canhdominich.boot.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OpsBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = OpsDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void byDefaultOnlyHealthIsExposedOverHttp() {
        assertThat(lines).contains(
                "GET /actuator -> 200 links [health, self]",
                "GET /actuator/health -> 200 {\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}",
                "GET /actuator/metrics -> 404",
                "GET /actuator/env -> 404");
    }

    @Test
    void healthShowsComponentsAndACustomIndicatorCanTakeTheServiceDown() {
        assertThat(line("GET /actuator/health -> 200 components")).contains("queue", "diskSpace", "livenessState", "readinessState");
        assertThat(lines).contains("queue component: \"queue\":{\"details\":{\"depth\":0,\"limit\":1000},\"status\":\"UP\"}");
        assertThat(line("queue depth 5000")).isEqualTo("queue depth 5000 -> GET /actuator/health 503 \"queue\":{\"details\":{\"depth\":5000,\"limit\":1000},\"status\":\"DOWN\"}");
    }

    @Test
    void aCustomMeterAppearsInTheMetricsEndpoint() {
        assertThat(lines).contains("GET /actuator/metrics/orders.created after 3 orders -> \"value\":3.0", "jvm.memory.used is a known meter: true");
    }

    @Test
    void envValuesAreMaskedUnlessYouAskToShowThemAndThenEvenPasswordsAreShown() {
        assertThat(lines.stream().filter(l -> l.startsWith("env app.")).toList()).hasSize(4);
        assertThat(lines.stream().filter(l -> l.startsWith("env app.")).toList().subList(0, 2)).allMatch(l -> l.endsWith("\"value\":\"******\"}"));
        assertThat(lines).contains("env app.name -> \"value\":\"Course Platform\"", "env app.password -> \"value\":\"hunter2\"");
    }

    @Test
    void aLogLevelChangesWhileTheApplicationRuns() {
        assertThat(lines).contains(
                "default level: [INFO order accepted]",
                "POST /actuator/loggers/dev.canhdominich.boot.ops.OrderController DEBUG -> 204",
                "after DEBUG: [DEBUG order accepted, details for debugging, INFO order accepted]");
    }

    @Test
    void structuredLoggingWritesJsonWithNamedFieldsAndDevtoolsStaysOutOfTheWayUnderJUnit() {
        assertThat(line("logstash format")).isEqualTo("logstash format, keys of one line: @timestamp, @version, level, level_value, logger_name, message, thread_name");
        assertThat(line("class loader of the application classes")).endsWith("-> AppClassLoader");
    }
}
