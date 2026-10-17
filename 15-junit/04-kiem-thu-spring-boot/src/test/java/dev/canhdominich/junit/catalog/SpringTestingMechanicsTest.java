package dev.canhdominich.junit.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringTestingMechanicsTest {

    private static final List<String> REPORT = new ArrayList<>();

    @AfterAll
    static void print() {
        REPORT.forEach(System.out::println);
    }

    private record Run(String line, long millis) {

        String contextId() {
            Matcher matcher = Pattern.compile("context (\\w+)").matcher(line);
            matcher.find();
            return matcher.group(1);
        }

        int beans() {
            Matcher matcher = Pattern.compile("beans (\\d+)").matcher(line);
            matcher.find();
            return Integer.parseInt(matcher.group(1));
        }
    }

    private static Run run(Class<?> probe) {
        Probes.LOG.clear();
        long start = System.nanoTime();
        List<ProbeRunner.Outcome> outcomes = ProbeRunner.run(probe);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(outcomes).extracting(ProbeRunner.Outcome::status).containsOnly("SUCCESSFUL");
        return new Run(Probes.LOG.get(0), millis);
    }

    @Test
    @Order(1)
    void identicalConfigurationReusesTheContextAndAMockBeanChangesIt() {
        REPORT.add("-- the context cache");
        run(Probes.FullContextA.class);
        Run first = run(Probes.FullContextA.class);
        Run second = run(Probes.FullContextB.class);
        Run withMock = run(Probes.FullContextWithAMock.class);
        REPORT.add("   two classes with the same configuration share one context: " + first.contextId().equals(second.contextId()));
        REPORT.add("   adding a @MockitoBean makes another context: " + !first.contextId().equals(withMock.contextId()));
        REPORT.add("   the second class started much faster than the one with a new context: " + (withMock.millis() > 5 * second.millis()));
        assertThat(first.contextId()).isEqualTo(second.contextId());
        assertThat(withMock.contextId()).isNotEqualTo(first.contextId());
        assertThat(withMock.millis()).isGreaterThan(5 * second.millis());
    }

    @Test
    @Order(2)
    void slicesLoadFewerBeansThanTheFullApplication() {
        REPORT.add("-- slices against the full application");
        Run full = run(Probes.FullContextA.class);
        Run web = run(Probes.WebSlice.class);
        Run jpa = run(Probes.JpaSlice.class);
        REPORT.add("   full application: " + full.beans() + " beans; " + full.line().replaceAll(".*beans \\d+, ", ""));
        REPORT.add("   @WebMvcTest: " + web.beans() + " beans; " + web.line().replaceAll(".*beans \\d+, ", ""));
        REPORT.add("   @DataJpaTest: " + jpa.beans() + " beans; " + jpa.line().replaceAll(".*beans \\d+, ", ""));
        assertThat(web.beans()).isLessThan(full.beans());
        assertThat(jpa.beans()).isLessThan(web.beans());
        assertThat(web.line()).contains("has controller true").contains("has repository false");
        assertThat(jpa.line()).contains("has controller false").contains("has repository true");
    }

    @Test
    @Order(3)
    void aTestTransactionRollsBackMockMvcRequestsButNotRealHttpRequests() {
        REPORT.add("-- @Transactional on the test class");
        Probes.LOG.clear();
        ProbeRunner.run(Probes.RollbackWithMockMvc.class);
        List<String> mock = List.copyOf(Probes.LOG);
        Probes.LOG.clear();
        ProbeRunner.run(Probes.RollbackWithARealServer.class);
        List<String> real = List.copyOf(Probes.LOG);
        mock.forEach(entry -> REPORT.add("   MockMvc: " + entry));
        real.forEach(entry -> REPORT.add("   real HTTP server: " + entry));
        assertThat(mock).containsExactly("inside the test: 1", "in the next test: 0");
        assertThat(real).containsExactly("HTTP status 201, inside the test: 1", "in the next test: 1");
    }
}
