package dev.canhdominich.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = ConfigDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void serverMergesFilesFromMostToLeastSpecific() {
        assertThat(line("GET /orders/default")).endsWith("[orders.yml, application.yml]");
        assertThat(line("GET /orders/prod")).endsWith("[orders-prod.yml, orders.yml, application.yml]");
    }

    @Test
    void clientReadsMergedValues() {
        assertThat(line("profile default")).endsWith("maxItems=10 currency=VND");
        assertThat(line("profile prod")).endsWith("maxItems=50 currency=VND");
    }

    @Test
    void changeNeedsARefreshAndOnlyRefreshScopeBeansSeeIt() {
        assertThat(line("server now serves")).endsWith("25");
        assertThat(line("client before refresh")).endsWith("maxItems=10 currency=VND");
        assertThat(line("POST /actuator/refresh")).endsWith("[orders.max-items]");
        assertThat(line("client after refresh")).endsWith("maxItems=25 currency=VND");
        assertThat(line("bean without @RefreshScope")).endsWith("maxItems=10");
    }

    @Test
    void serverBeatsLocalDefaultsButNotCommandLine() {
        assertThat(line("local default property")).endsWith("maxItems=10 currency=VND");
        assertThat(line("command-line argument")).endsWith("maxItems=99 currency=VND");
    }

    @Test
    void encryptedValuesAreDecryptedByTheServer() {
        assertThat(line("encrypted value")).endsWith("true");
        assertThat(line("server answers with the decrypted value")).endsWith("s3cret-value");
    }

    @Test
    void unreachableServerIsOptionalOrFatalByConfiguration() {
        assertThat(line("optional import")).endsWith("maxItems=unset currency=unset");
        assertThat(line("required import")).endsWith("ConfigClientFailFastException");
    }
}
