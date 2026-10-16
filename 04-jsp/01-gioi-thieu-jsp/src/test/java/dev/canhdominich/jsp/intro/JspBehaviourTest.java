package dev.canhdominich.jsp.intro;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JspBehaviourTest {

    private static EmbeddedServer server;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        server = EmbeddedServer.start(0);
        client = HttpClient.newHttpClient();
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void jspIsTranslatedIntoAServletAndOnlyTheFirstRequestPaysForCompiling() throws Exception {
        long start = System.nanoTime();
        HttpResponse<String> first = get("/hello.jsp");
        long firstMs = (System.nanoTime() - start) / 1_000_000;
        start = System.nanoTime();
        get("/hello.jsp");
        long secondMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains("Xin chào JSP").contains("Tổng 1 + 2 tính bằng EL: 3").contains("<li>dòng 3</li>");
        assertThat(firstMs).isGreaterThan(secondMs * 3);
        List<String> generated = JspProbes.generatedLines(server, "hello_jsp.java");
        assertThat(generated).anyMatch(line -> line.startsWith("public final class hello_jsp extends org.apache.jasper.runtime.HttpJspBase"))
                .anyMatch(line -> line.startsWith("public void _jspService"))
                .anyMatch(line -> line.startsWith("for (int i = 1; i <= 3; i++)"))
                .anyMatch(line -> line.startsWith("out.print( i );"))
                .anyMatch(line -> line.contains("proprietaryEvaluate(\"${1 + 2}\""));
    }

    @Test
    void declarationBecomesASharedFieldSoItLosesUpdatesUnderConcurrency() throws Exception {
        int value = Integer.parseInt(JspProbes.hammer(client, base));
        assertThat(value).isLessThan(JspProbes.INCREMENTS);
    }

    @Test
    void pageCreatesASessionUnlessItOptsOut() throws Exception {
        assertThat(get("/session-default.jsp").headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie -> assertThat(cookie).startsWith("JSESSIONID="));
        assertThat(get("/session-off.jsp").headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void scriptletAndElPrintRawHtmlButCOutEscapesIt() throws Exception {
        String body = get("/xss.jsp?q=%3Cscript%3Ealert(1)%3C/script%3E").body();
        assertThat(body).contains("scriptlet : <script>alert(1)</script>").contains("EL        : <script>alert(1)</script>").contains("c:out     : &lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void editedPageIsPickedUpOnlyAfterTheModificationCheckInterval() throws Exception {
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("jsp-dynamic-test");
        java.nio.file.Path page = directory.resolve("dynamic.jsp");
        java.nio.file.Files.writeString(page, "<%@ page contentType=\"text/plain\" session=\"false\" %>version 1\n");
        var context = server.tomcat().addWebapp("/dyn", directory.toString());
        assertThat(get("/dyn/dynamic.jsp").body().trim()).isEqualTo("version 1");
        java.nio.file.Files.writeString(page, "<%@ page contentType=\"text/plain\" session=\"false\" %>version 2\n");
        java.nio.file.Files.setLastModifiedTime(page, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 5000));
        assertThat(get("/dyn/dynamic.jsp").body().trim()).isEqualTo("version 1");
        Thread.sleep(5000);
        assertThat(get("/dyn/dynamic.jsp").body().trim()).isEqualTo("version 2");
        context.getParent().removeChild(context);
    }

    @Test
    void compileErrorPageShowsSourceLinesToTheClient() throws Exception {
        HttpResponse<String> broken = get("/broken.jsp");
        assertThat(broken.statusCode()).isEqualTo(500);
        assertThat(broken.body()).contains("An error occurred at line: [2] in the jsp file").contains("Unable to compile class for JSP").contains("secretPrice");
    }
}
