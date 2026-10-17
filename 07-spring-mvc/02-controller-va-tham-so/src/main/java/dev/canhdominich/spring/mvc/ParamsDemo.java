package dev.canhdominich.spring.mvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ParamsDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        try (EmbeddedServer server = EmbeddedServer.start(0, WebConfig.class)) {
            Client client = new Client(server.port(), lines);
            client.get("/pages/students/5");
            client.get("/pages/students/new");
            client.get("/pages/products/J-77/reviews/3");
            client.get("/pages/search?q=java&page=2");
            client.get("/pages/search?q=java");
            client.get("/pages/search");
            client.get("/pages/search?q=java&page=abc");
            client.get("/pages/students/abc");
            client.get("/pages/trace");
            client.get("/pages/trace", "X-Trace", "abc-123");
            client.post("/pages/register", "application/x-www-form-urlencoded", "name=An&age=20");
            client.post("/pages/register", "application/x-www-form-urlencoded", "name=An&age=abc");
            client.get("/pages/view");
            client.get("/api/students");
            client.get("/api/students/1");
            client.get("/api/students/9");
            client.post("/api/students", "application/json", "{\"name\":\"Chi\"}");
            client.post("/api/students", "text/plain", "Chi");
            client.get("/api/students/1", "Accept", "text/plain");
        }
        return lines;
    }

    static final class Client {

        private final int port;
        private final List<String> lines;
        private final HttpClient http = HttpClient.newHttpClient();

        Client(int port, List<String> lines) {
            this.port = port;
            this.lines = lines;
        }

        void get(String path, String... headers) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
            for (int i = 0; i < headers.length; i += 2) {
                builder.header(headers[i], headers[i + 1]);
            }
            send("GET " + path + (headers.length > 0 ? " [" + headers[0] + ": " + headers[1] + "]" : ""), builder.build());
        }

        void post(String path, String contentType, String body) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            send("POST " + path + " [" + contentType + "] " + body, request);
        }

        private void send(String label, HttpRequest request) throws Exception {
            Trace.clear();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            StringBuilder out = new StringBuilder(label + " -> " + response.statusCode());
            response.headers().firstValue("Content-Type").ifPresent(type -> out.append(" ").append(type.split(";")[0]));
            response.headers().firstValue("Location").ifPresent(location -> out.append(" Location: ").append(location));
            String body = response.body().replaceAll("\\s+", " ").trim();
            if (response.statusCode() < 400 && !body.isEmpty()) {
                out.append(" | ").append(body);
            }
            if (!Trace.all().isEmpty()) {
                out.append(" | spring raised ").append(Trace.all().get(0));
            }
            System.out.println(out);
            lines.add(out.toString());
        }
    }
}
