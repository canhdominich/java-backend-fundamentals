package dev.canhdominich.intro.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CompareDemo {

    private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
        System.exit(0);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        try (RawHttpServer raw = new RawHttpServer(); ServletServer servlet = new ServletServer()) {
            lines.add("-- four requests at the same time, each takes 1 s on the server");
            lines.add("hand-written server on a socket: all four done after about " + concurrent(raw.port()) + " s");
            lines.add("servlet container: all four done after about " + concurrent(servlet.port()) + " s");
            lines.add("-- GET /hello?name=An%20Binh");
            lines.add("hand-written server answers: " + send("GET", raw.port(), "/hello?name=An%20Binh", null).body());
            lines.add("servlet container answers: " + send("GET", servlet.port(), "/hello?name=An%20Binh", null).body());
            lines.add("-- reading the header User-Agent: lab as \"user-agent\"");
            lines.add("hand-written server: " + send("GET", raw.port(), "/agent", "lab").body());
            lines.add("servlet container: " + send("GET", servlet.port(), "/agent", "lab").body());
            lines.add("-- POST to a path whose handler only knows GET");
            lines.add("hand-written server: status " + send("POST", raw.port(), "/hello?name=x", null).statusCode());
            lines.add("servlet container: status " + send("POST", servlet.port(), "/hello?name=x", null).statusCode());
            lines.add("-- a path nobody handles");
            lines.add("hand-written server: status " + send("GET", raw.port(), "/missing", null).statusCode());
            lines.add("servlet container: status " + send("GET", servlet.port(), "/missing", null).statusCode());
        }
        return lines;
    }

    private static String concurrent(int port) throws Exception {
        long start = System.nanoTime();
        List<java.util.concurrent.CompletableFuture<HttpResponse<String>>> calls = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            calls.add(HTTP.sendAsync(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/slow")).GET().build(), HttpResponse.BodyHandlers.ofString()));
        }
        for (var call : calls) {
            call.get();
        }
        double seconds = (System.nanoTime() - start) / 1e9;
        return String.valueOf(Math.round(seconds * 2) / 2.0);
    }

    private static HttpResponse<String> send(String method, int port, String path, String userAgent) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).method(method, HttpRequest.BodyPublishers.noBody());
        if (userAgent != null) {
            request.header("User-Agent", userAgent);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
