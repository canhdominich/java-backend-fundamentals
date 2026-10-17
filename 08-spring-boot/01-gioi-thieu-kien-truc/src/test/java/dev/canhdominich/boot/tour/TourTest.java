package dev.canhdominich.boot.tour;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = TourDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void bootFourPointOneManagesSpringSevenAndTomcatEleven() {
        assertThat(line("spring boot")).startsWith("spring boot 4.1.1, spring framework 7.0.9, tomcat 11.0.24");
    }

    @Test
    void onlyTheProjectsOwnClassesAreDeclaredByHand() {
        assertThat(line("beans in the context")).endsWith("declared by this project's classes: [application, helloController]");
    }

    @Test
    void autoConfigurationRegistersWhatTheManualMvcLabHadToWire() {
        assertThat(lines).contains(
                "  DispatcherServletAutoConfiguration: applied",
                "  TomcatServletWebServerAutoConfiguration: applied",
                "  WebMvcAutoConfiguration: applied",
                "  JacksonAutoConfiguration: applied",
                "  HttpMessageConvertersAutoConfiguration: applied",
                "  ErrorMvcAutoConfiguration: applied");
        assertThat(line("auto-configurations whose conditions did not match")).contains("TomcatReactiveWebServerAutoConfiguration");
        assertThat(line("handler mappings")).contains("requestMappingHandlerMapping", "welcomePageHandlerMapping");
    }

    @Test
    void theRunningServerAnswersWithJsonAndAJsonErrorBody() {
        assertThat(line("GET /hello")).isEqualTo("GET /hello -> 200 application/json {\"message\":\"Xin chao Spring Boot\"}");
        assertThat(line("GET /nope")).isEqualTo("GET /nope -> 404 application/json {\"status\":404,\"error\":\"Not Found\",\"path\":\"/nope\"}");
        assertThat(line("embedded server")).endsWith("true");
    }
}
