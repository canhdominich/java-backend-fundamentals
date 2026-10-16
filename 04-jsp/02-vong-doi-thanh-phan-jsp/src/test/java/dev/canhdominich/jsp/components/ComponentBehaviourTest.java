package dev.canhdominich.jsp.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ComponentBehaviourTest {

    private static EmbeddedServer server;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        server = EmbeddedServer.start(0);
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    private static HttpClient fresh() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private static HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void jspInitRunsOnceJspServiceRunsPerRequestAndJspDestroyRunsAtStop() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try (EmbeddedServer own = EmbeddedServer.start(0)) {
            HttpClient client = HttpClient.newHttpClient();
            for (int i = 0; i < 3; i++) {
                client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + own.port() + "/lifecycle.jsp")).build(), HttpResponse.BodyHandlers.ofString());
            }
        } finally {
            System.setOut(original);
        }
        String log = captured.toString();
        assertThat(log.split("jspInit\\(\\) called", -1)).hasSize(2);
        assertThat(log.split("_jspService\\(\\) called", -1)).hasSize(4);
        assertThat(log).contains("jspDestroy() called");
    }

    @Test
    void staticIncludeIsMergedIntoTheServletAndDynamicIncludeIsADispatch() throws Exception {
        HttpResponse<String> response = get(fresh(), "/includes.jsp");
        assertThat(response.body()).contains("static header text").contains("static include shares variables: defined in the static header")
                .contains("dynamic header, param title = from the including page");
        String source = ComponentProbes.generatedSource(server, "includes_jsp.java");
        assertThat(source).contains("out.write(\"<header>static header text</header>").contains("JspRuntimeLibrary.include(request, response, _jspx_temp0_url");
    }

    @Test
    void dynamicIncludeReachesTheFilterWithDispatcherTypeInclude() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            get(fresh(), "/includes.jsp");
        } finally {
            System.setOut(original);
        }
        assertThat(captured.toString()).contains("dispatcher type INCLUDE");
    }

    @Test
    void elReadsScopesFromNarrowestToWidest() throws Exception {
        assertThat(get(fresh(), "/scopes?set=").body().trim()).isEqualTo("||||");
        assertThat(get(fresh(), "/scopes?set=application").body().trim()).isEqualTo("application||||application");
        assertThat(get(fresh(), "/scopes?set=session,application").body().trim()).isEqualTo("session|||session|application");
        assertThat(get(fresh(), "/scopes?set=request,session,application").body().trim()).isEqualTo("request||request|session|application");
        assertThat(get(fresh(), "/scopes?set=request,session,application&page=yes").body().trim()).isEqualTo("page|page|request|session|application");
    }

    @Test
    void sessionScopeIsEmptyInAPageThatOptedOutOfTheSession() throws Exception {
        HttpClient client = fresh();
        get(client, "/scopes?set=session");
        assertThat(get(client, "/scopes.jsp").body().trim()).startsWith("session|||session|");
        assertThat(get(client, "/scopes-nosession.jsp").body().trim()).doesNotContain("session|").startsWith("application||||");
    }

    @Test
    void elTreatsRecordsAsBeansAndMissingValuesAsEmptyButMissingPropertiesAsErrors() throws Exception {
        String body = get(fresh(), "/products").body();
        assertThat(body).contains("<td>KB-01</td>").contains("<td>Mouse &lt;b&gt;Pro&lt;/b&gt;</td>").contains("<td>sold out</td>").contains("<td>unknown</td>")
                .contains("missing variable: []").contains("property of a missing variable: []").contains("empty test: true")
                .contains("Property [nothing] not found on type [dev.canhdominich.jsp.components.Product]");
    }

    @Test
    void currencyFormattingRoundsToTheLocaleFractionDigits() throws Exception {
        String body = get(fresh(), "/products").body().replace('\u00a0', ' ').replace('\u202f', ' ');
        assertThat(body).contains("<td>50 ₫</td>").contains("<td>20 ₫</td>").contains("<td>1.299 ₫</td>");
    }

    @Test
    void errorPageReceivesTheExceptionAndKeepsStatus500() throws Exception {
        HttpResponse<String> response = get(fresh(), "/throws.jsp");
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("exception class : java.lang.IllegalStateException").contains("the report generator failed").contains("response status  : 500");
    }

    @Test
    void useBeanKeepsTheBeanInTheSession() throws Exception {
        HttpClient client = fresh();
        assertThat(get(client, "/bean.jsp").body()).contains("owner=guest items=1");
        assertThat(get(client, "/bean.jsp").body()).contains("owner=guest items=2");
        assertThat(get(fresh(), "/bean.jsp").body()).contains("items=1");
    }
}
