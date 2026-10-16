package dev.canhdominich.servlet.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FilterBehaviourTest {

    private static EmbeddedServer server;
    private static HttpClient client;
    private static HttpClient following;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        server = EmbeddedServer.start(0);
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        following = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    private static HttpResponse<String> get(HttpClient httpClient, String path, String... headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
        for (int i = 0; i + 1 < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String events(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    private static Runnable call(String path, String... headers) {
        return () -> {
            try {
                get(client, path, headers);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Test
    void servletConfigAndServletContextSeeDifferentParameters() throws Exception {
        String body = get(client, "/config").body();
        assertThat(body).contains("ServletConfig  greeting      = Xin chào")
                .contains("ServletConfig  appName       = null")
                .contains("ServletContext appName       = Course Shop")
                .contains("ServletContext greeting      = null")
                .contains("startedBy     = AppListener for Course Shop");
    }

    @Test
    void servletsCanBeRegisteredInWebXmlAndInCode() throws Exception {
        assertThat(get(client, "/health").body()).contains("registered by web.xml at /health");
        assertThat(get(client, "/programmatic").body()).contains("registered by ServletContext.addServlet at /programmatic");
    }

    @Test
    void forwardKeepsRequestAttributesAndOriginalUriButRedirectLosesThem() throws Exception {
        HttpResponse<String> forward = get(client, "/dispatch/forward");
        assertThat(forward.statusCode()).isEqualTo(202);
        assertThat(forward.headers().firstValue("X-Set-By-Target")).contains("yes");
        assertThat(forward.body()).contains("getRequestURI    = /dispatch/target").contains("forward original = /dispatch/forward")
                .contains("message attribute = set before forward").doesNotContain("this text is discarded");

        HttpResponse<String> redirect = get(client, "/dispatch/redirect");
        assertThat(redirect.statusCode()).isEqualTo(302);
        assertThat(get(following, "/dispatch/redirect").body()).contains("message attribute = null");
    }

    @Test
    void includeKeepsTheOuterUriAndIgnoresStatusAndHeadersFromTheIncludedResource() throws Exception {
        HttpResponse<String> include = get(client, "/dispatch/include");
        assertThat(include.statusCode()).isEqualTo(200);
        assertThat(include.headers().firstValue("X-Set-By-Target")).isEmpty();
        assertThat(include.body()).contains("--- before include").contains("getRequestURI    = /dispatch/include")
                .contains("include uri attr = /dispatch/target").contains("--- after include");
    }

    @Test
    void filtersWrapTheServletInMappingOrderAndUnwindInReverse() {
        String log = events(call("/secure/data", "X-Token", "secret"));
        assertThat(log).containsSubsequence("logging filter: before /secure/data", "encoding filter", "auth filter: token accepted", "secure servlet runs", "logging filter: after /secure/data, status 200");
    }

    @Test
    void aFilterThatDoesNotContinueTheChainStopsTheServlet() throws Exception {
        String log = events(call("/secure/data"));
        assertThat(log).contains("auth filter: rejected").doesNotContain("secure servlet runs").contains("logging filter: after /secure/data, status 401");
        assertThat(get(client, "/secure/data").statusCode()).isEqualTo(401);
    }

    @Test
    void filtersAreMappedToRequestDispatchOnlyUnlessToldOtherwise() {
        String forward = events(call("/dispatch/forward"));
        assertThat(forward).contains("forwardLogging filter: dispatcher type FORWARD");
        assertThat(forward.split("logging filter: before", -1)).hasSize(2);
        assertThat(events(call("/dispatch/include"))).contains("forwardLogging filter: dispatcher type INCLUDE");
        assertThat(events(call("/dispatch/target"))).contains("forwardLogging filter: dispatcher type REQUEST");
    }

    @Test
    void annotatedFiltersRunInAnOrderTheSpecificationDoesNotPromise() {
        String log = events(call("/annotated"));
        assertThat(log).contains("annotated filter apple", "annotated filter zebra");
    }

    @Test
    void requestWrapperLetsTheBodyBeReadTwice() throws Exception {
        HttpResponse<String> response = following.send(HttpRequest.newBuilder(URI.create(base + "/api/echo")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"KB-01\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).contains("servlet read 15 characters, a second read gave 15").contains("CachedBodyRequest");
    }

    @Test
    void frontControllerRoutesByPathInfoAndAnswers404ForUnknownRoutes() throws Exception {
        assertThat(get(client, "/app/orders").body()).contains("-> order list");
        assertThat(get(client, "/app/orders/42").body()).contains("-> order 42");
        HttpResponse<String> unknown = get(client, "/app/unknown");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(unknown.body()).contains("no handler for /unknown");
    }

    @Test
    void sessionListenerSeesCreationAndInvalidation() throws Exception {
        String[] cookie = new String[1];
        String created = events(() -> {
            try {
                cookie[0] = get(client, "/config?session=1").headers().firstValue("Set-Cookie").orElseThrow();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertThat(created).contains("session created, active=1");
        String destroyed = events(call("/config?logout=1", "Cookie", cookie[0].substring(0, cookie[0].indexOf(';'))));
        assertThat(destroyed).contains("session destroyed, active=0");
    }
}
