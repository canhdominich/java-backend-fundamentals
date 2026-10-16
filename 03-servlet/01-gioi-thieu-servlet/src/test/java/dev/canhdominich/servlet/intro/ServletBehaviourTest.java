package dev.canhdominich.servlet.intro;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServletBehaviourTest {

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
    void helloServletReturnsUtf8Html() throws Exception {
        HttpResponse<String> response = get("/hello?name=An");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("text/html;charset=UTF-8");
        assertThat(response.body()).contains("Xin chào từ An!");
    }

    @Test
    void urlMappingFollowsExactThenLongestPathThenExtension() throws Exception {
        assertThat(get("/orders").body()).startsWith("ExactServlet servletPath=/orders pathInfo=null");
        assertThat(get("/orders/42/items").body()).startsWith("PrefixServlet servletPath=/orders pathInfo=/42/items");
        assertThat(get("/report.do").body()).startsWith("ExtensionServlet servletPath=/report.do");
        assertThat(get("/orders/x.do").body()).startsWith("PrefixServlet");
    }

    @Test
    void unmappedPathsAreAnsweredByTheBuiltInDefaultServletWith404() throws Exception {
        HttpResponse<String> response = get("/nowhere/page");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Apache Tomcat");
    }

    @Test
    void servletWithoutDoPostAnswers405() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/hello")).POST(HttpRequest.BodyPublishers.ofString("x=1")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    void defaultErrorPageLeaksMessageAndServerVersion() throws Exception {
        HttpResponse<String> response = get("/boom");
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("hunter2").contains("IllegalStateException").contains("Apache Tomcat");
    }

    @Test
    void oneServletInstanceServesManyThreads() throws Exception {
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            futures.add(client.sendAsync(HttpRequest.newBuilder(URI.create(base + "/who")).build(), HttpResponse.BodyHandlers.ofString()));
        }
        Set<String> instances = new HashSet<>();
        Set<String> threads = new HashSet<>();
        for (CompletableFuture<HttpResponse<String>> future : futures) {
            String[] parts = future.get().body().trim().split(",");
            instances.add(parts[0]);
            threads.add(parts[1]);
        }
        assertThat(instances).hasSize(1);
        assertThat(threads.size()).isGreaterThan(1);
    }

    @Test
    void rawHttpExchangeShowsStatusLineHeadersAndByteLength() throws Exception {
        String raw = RawHttp.exchange(server.port(), "GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
        assertThat(raw).startsWith("HTTP/1.1 200");
        assertThat(raw).contains("Content-Type: text/html;charset=UTF-8").contains("Content-Length: 33").contains("Connection: close");
    }
}
