package dev.canhdominich.intro.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CompareBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = CompareDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void aSingleThreadedServerServesRequestsOneAfterAnother() {
        assertThat(line("hand-written server on a socket")).endsWith("about 4.0 s");
        assertThat(line("servlet container: all four")).endsWith("about 1.0 s");
    }

    @Test
    void theContainerDecodesTheQueryString() {
        assertThat(line("hand-written server answers")).endsWith("hello An%20Binh");
        assertThat(line("servlet container answers")).endsWith("hello An Binh");
    }

    @Test
    void theContainerLooksUpHeadersCaseInsensitively() {
        assertThat(line("hand-written server: null")).isEqualTo("hand-written server: null");
        assertThat(line("servlet container: lab")).isEqualTo("servlet container: lab");
    }

    @Test
    void theContainerRejectsAMethodTheServletDoesNotImplement() {
        assertThat(line("hand-written server: status 200")).isEqualTo("hand-written server: status 200");
        assertThat(line("servlet container: status 405")).isEqualTo("servlet container: status 405");
    }

    @Test
    void bothAnswer404ForUnknownPaths() {
        assertThat(lines).containsSubsequence("-- a path nobody handles", "hand-written server: status 404", "servlet container: status 404");
    }
}
