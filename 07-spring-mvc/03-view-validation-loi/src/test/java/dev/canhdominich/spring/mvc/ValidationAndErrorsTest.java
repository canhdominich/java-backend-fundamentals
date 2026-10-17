package dev.canhdominich.spring.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidationAndErrorsTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = FormDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void invalidFormComesBackWithOneMessagePerField() {
        assertThat(line("POST /register name=&")).isEqualTo(
                "POST /register name=&email=x&age=10 -> 200 | errors [name: must not be blank, email: must be a well-formed email address, age: must be greater than or equal to 18]");
    }

    @Test
    void validFormRedirectsAndTheFlashMessageSurvivesExactlyOneRequest() {
        assertThat(line("POST /register name=An")).endsWith("-> 302 Location: /registered");
        assertThat(lines.stream().filter(l -> l.startsWith("GET /registered")).toList()).containsExactly(
                "GET /registered -> 200 | message=[Saved An] count=[1]",
                "GET /registered -> 200 | message=[] count=[1]");
    }

    @Test
    void answeringAPostWithoutRedirectRepeatsTheSaveWhenTheBrowserResends() {
        assertThat(lines.stream().filter(l -> l.startsWith("POST /register-no-redirect")).toList()).containsExactly(
                "POST /register-no-redirect name=Binh&email=binh%40example.com&age=30 -> 200 | message=[Saved Binh] count=[2]",
                "POST /register-no-redirect name=Binh&email=binh%40example.com&age=30 -> 200 | message=[Saved Binh] count=[3]");
    }

    @Test
    void validWithoutABindingResultParameterTurnsIntoA400() {
        assertThat(line("POST /register-strict")).endsWith("-> 400");
    }

    @Test
    void adviceTurnsExceptionsIntoPagesAndJsonAndALocalHandlerWins() {
        assertThat(line("GET /pages/course/9")).isEqualTo("GET /pages/course/9 -> 404 | error=[Course 9 does not exist]");
        assertThat(line("GET /local/course/9")).endsWith("| handled inside the controller: Course 9 does not exist");
        assertThat(line("GET /api/course/9")).endsWith("| {\"status\":404,\"error\":\"Course 9 does not exist\"}");
        assertThat(line("POST /api/users")).contains("-> 400").endsWith("\"fields\":{\"age\":\"must be greater than or equal to 18\",\"email\":\"must be a well-formed email address\",\"name\":\"must not be blank\"}}");
    }

    @Test
    void anUnexpectedExceptionDoesNotLeakItsMessage() {
        assertThat(line("GET /api/boom")).isEqualTo("GET /api/boom -> 500 | {\"status\":500,\"error\":\"Internal server error\"}").doesNotContain("hunter2");
    }
}
