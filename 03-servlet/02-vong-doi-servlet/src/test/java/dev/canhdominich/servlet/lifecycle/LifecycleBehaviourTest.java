package dev.canhdominich.servlet.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LifecycleBehaviourTest {

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

    private static HttpResponse<String> send(String method, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void eagerServletInitialisesAtStartupAndLazyServletOnFirstRequestOnly() throws Exception {
        String before = get("/events").body();
        assertThat(before).contains("EagerServlet.init()").doesNotContain("LazyServlet.init()");
        get("/life/lazy");
        get("/life/lazy");
        String after = get("/events").body();
        assertThat(after.split("LazyServlet.init\\(\\)", -1)).hasSize(2);
        assertThat(after).contains("LazyServlet.doGet()");
    }

    @Test
    void webappClassesAreIsolatedFromTheLauncherClasspath() throws Exception {
        get("/life/eager");
        assertThat(get("/events").body()).contains("EagerServlet.doGet()");
        assertThat(EventLog.snapshot()).isEmpty();
    }

    @Test
    void ordinaryInitFailureIsRetriedOnTheNextRequestButUnavailableIsPermanent() throws Exception {
        assertThat(get("/bad-init/ordinary").statusCode()).isEqualTo(500);
        HttpResponse<String> second = get("/bad-init/ordinary");
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).contains("ready after 2 init attempts");
        assertThat(get("/bad-init/unavailable").statusCode()).isEqualTo(404);
        assertThat(get("/bad-init/unavailable").statusCode()).isEqualTo(404);
    }

    @Test
    void containerAddsOptionsAndHeadOnTopOfDoGetAndDoPost() throws Exception {
        HttpResponse<String> options = send("OPTIONS", "/methods");
        assertThat(options.headers().firstValue("Allow")).hasValueSatisfying(allow -> assertThat(allow).contains("GET", "HEAD", "POST", "OPTIONS"));
        HttpResponse<String> head = send("HEAD", "/methods");
        assertThat(head.statusCode()).isEqualTo(200);
        assertThat(head.body()).isEmpty();
        assertThat(head.headers().firstValue("Content-Length")).contains("17");
    }

    @Test
    void sharedFieldLosesUpdatesUnlessItIsAtomicOrSynchronized() throws Exception {
        assertThat(LifecycleProbes.hammer(client, base, "atomic")).isEqualTo(String.valueOf(LifecycleProbes.INCREMENTS));
        assertThat(LifecycleProbes.hammer(client, base, "synchronized")).isEqualTo(String.valueOf(LifecycleProbes.INCREMENTS));
        int unsafe = Integer.parseInt(LifecycleProbes.hammer(client, base, "unsafe"));
        assertThat(unsafe).isLessThan(LifecycleProbes.INCREMENTS);
    }

    @Test
    void destroyRunsWhenTheServerStops() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (EmbeddedServer second = EmbeddedServer.start(0)) {
            HttpClient localClient = HttpClient.newHttpClient();
            localClient.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + second.port() + "/life/lazy")).build(), HttpResponse.BodyHandlers.ofString());
            System.setOut(new PrintStream(captured, true));
        } finally {
            System.setOut(original);
        }
        assertThat(captured.toString()).contains("LazyServlet.destroy()").contains("EagerServlet.destroy()");
    }
}
