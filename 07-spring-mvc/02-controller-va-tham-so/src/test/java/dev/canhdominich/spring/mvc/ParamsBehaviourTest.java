package dev.canhdominich.spring.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ParamsBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = ParamsDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void pathVariablesAreConvertedAndTheLiteralPathBeatsThePattern() {
        assertThat(line("GET /pages/students/5 ")).endsWith("| student 5");
        assertThat(line("GET /pages/students/new ")).endsWith("| form for a new student");
        assertThat(line("GET /pages/products/J-77/reviews/3")).endsWith("| review 3 of J-77");
    }

    @Test
    void requestParametersUseDefaultsAndRejectMissingOrMalformedValues() {
        assertThat(line("GET /pages/search?q=java&page=2")).endsWith("| q=java page=2");
        assertThat(line("GET /pages/search?q=java ")).endsWith("| q=java page=1");
        assertThat(line("GET /pages/search -> ")).startsWith("GET /pages/search -> 400").endsWith("spring raised MissingServletRequestParameterException");
        assertThat(line("GET /pages/search?q=java&page=abc")).contains("-> 400").endsWith("spring raised MethodArgumentTypeMismatchException");
        assertThat(line("GET /pages/students/abc")).contains("-> 400").endsWith("spring raised MethodArgumentTypeMismatchException");
    }

    @Test
    void headersAndFormFieldsBindToParametersAndObjects() {
        assertThat(line("GET /pages/trace -> ")).endsWith("| trace=none");
        assertThat(line("GET /pages/trace [X-Trace")).endsWith("| trace=abc-123");
        assertThat(line("POST /pages/register [application/x-www-form-urlencoded] name=An&age=20")).endsWith("| registered An aged 20");
        assertThat(line("POST /pages/register [application/x-www-form-urlencoded] name=An&age=abc")).contains("-> 400").endsWith("spring raised MethodArgumentNotValidException");
    }

    @Test
    void aControllerRendersAViewAndARestControllerWritesJson() {
        assertThat(line("GET /pages/view")).contains("text/html").endsWith("<h1>Danh sach sinh vien</h1></body></html>");
        assertThat(line("GET /api/students -> ")).contains("application/json").endsWith("| [{\"id\":1,\"name\":\"An\"},{\"id\":2,\"name\":\"Binh\"}]");
        assertThat(line("GET /api/students/1 -> ")).endsWith("| {\"id\":1,\"name\":\"An\"}");
        assertThat(line("GET /api/students/9")).isEqualTo("GET /api/students/9 -> 404");
    }

    @Test
    void contentNegotiationProducesCreated415And406() {
        assertThat(line("POST /api/students [application/json]")).contains("-> 201 application/json Location: /api/students/3").endsWith("| {\"id\":3,\"name\":\"Chi\"}");
        assertThat(line("POST /api/students [text/plain]")).contains("-> 415").endsWith("spring raised HttpMediaTypeNotSupportedException");
        assertThat(line("GET /api/students/1 [Accept: text/plain]")).contains("-> 406").endsWith("spring raised HttpMediaTypeNotAcceptableException");
    }
}
