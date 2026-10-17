package dev.canhdominich.boot.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RestBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = RestDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void createReturns201WithLocationAndTheCreatedResource() {
        assertThat(line("POST /api/courses {\"title\":\"Spring Boot\"")).isEqualTo(
                "POST /api/courses {\"title\":\"Spring Boot\",\"price\":100} -> 201 application/json Location: /api/courses/1 | {\"id\":1,\"title\":\"Spring Boot\",\"price\":100}");
    }

    @Test
    void validationAndReadingErrorsBecomeProblemDetailBodies() {
        assertThat(line("POST /api/courses {\"title\":\"\"")).contains("-> 400 application/problem+json").endsWith(
                "{\"detail\":\"Request validation failed\",\"status\":400,\"title\":\"Bad Request\",\"errors\":{\"price\":\"must be greater than or equal to 0\",\"title\":\"must not be blank\"}}");
        assertThat(line("POST /api/courses {\"title\": ->")).contains("-> 400 application/problem+json").contains("Failed to read request");
        assertThat(line("POST /api/courses Spring Boot")).contains("-> 415 application/problem+json");
    }

    @Test
    void unknownJsonFieldsAreIgnoredByBootsJacksonDefaults() {
        assertThat(line("POST /api/courses {\"title\":\"Extra field ignored\"")).contains("-> 201").endsWith("{\"id\":2,\"title\":\"Extra field ignored\",\"price\":5}");
    }

    @Test
    void readingMissingAndPagedResources() {
        assertThat(line("GET /api/courses/1 ->")).endsWith("{\"id\":1,\"title\":\"Spring Boot\",\"price\":100}");
        assertThat(line("GET /api/courses/99")).contains("-> 404 application/problem+json").contains("Course 99 does not exist");
        assertThat(line("GET /api/courses?page=1&size=5")).endsWith("\"page\":1,\"size\":5,\"total\":11}");
        assertThat(line("GET /api/courses?size=0")).contains("-> 400 application/problem+json").contains("Validation failure");
        assertThat(line("GET /api/courses?size=abc")).contains("-> 400").contains("Failed to convert 'size' with value: 'abc'");
    }

    @Test
    void updateDeleteAndTheStatusCodesOfRepeatedAndUnsupportedCalls() {
        assertThat(line("PUT /api/courses/1")).contains("-> 200").endsWith("{\"id\":1,\"title\":\"Spring Boot 4\",\"price\":150}");
        assertThat(line("PUT /api/courses/99")).contains("-> 404");
        assertThat(lines.stream().filter(l -> l.startsWith("DELETE /api/courses/1")).map(l -> l.substring(0, "DELETE /api/courses/1 -> 204".length())).toList())
                .containsExactly("DELETE /api/courses/1 -> 204", "DELETE /api/courses/1 -> 404");
        assertThat(line("PATCH /api/courses/2")).contains("-> 405 application/problem+json Allow: DELETE, GET, PUT");
        assertThat(line("GET /api/courses/2 ->")).contains("-> 406 application/problem+json");
    }
}
