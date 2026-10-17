package dev.canhdominich.boot.rest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class RestDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(RestApplication.class);
        application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "OFF"));
        try (ConfigurableApplicationContext context = application.run()) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            Client client = new Client(port, lines);
            client.send("POST", "/api/courses", "application/json", "{\"title\":\"Spring Boot\",\"price\":100}");
            client.send("POST", "/api/courses", "application/json", "{\"title\":\"\",\"price\":-1}");
            client.send("POST", "/api/courses", "application/json", "{\"title\":\"Extra field ignored\",\"price\":5,\"unknown\":true}");
            client.send("POST", "/api/courses", "application/json", "{\"title\":");
            client.send("POST", "/api/courses", "text/plain", "Spring Boot");
            for (int i = 4; i <= 12; i++) {
                client.quiet("POST", "/api/courses", "application/json", "{\"title\":\"Course " + i + "\",\"price\":" + i + "}");
            }
            client.send("GET", "/api/courses/1", null, null);
            client.send("GET", "/api/courses/99", null, null);
            client.send("GET", "/api/courses?page=1&size=5", null, null);
            client.send("GET", "/api/courses?size=0", null, null);
            client.send("GET", "/api/courses?size=abc", null, null);
            client.send("PUT", "/api/courses/1", "application/json", "{\"title\":\"Spring Boot 4\",\"price\":150}");
            client.send("PUT", "/api/courses/99", "application/json", "{\"title\":\"x\",\"price\":1}");
            client.send("DELETE", "/api/courses/1", null, null);
            client.send("DELETE", "/api/courses/1", null, null);
            client.send("PATCH", "/api/courses/2", "application/json", "{}");
            client.send("GET", "/api/courses/2", "Accept: application/xml", null);
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

        void quiet(String method, String path, String type, String body) throws Exception {
            request(method, path, type, body);
        }

        void send(String method, String path, String type, String body) throws Exception {
            HttpResponse<String> response = request(method, path, type, body);
            StringBuilder out = new StringBuilder(method + " " + path + (body == null ? "" : " " + body) + " -> " + response.statusCode());
            response.headers().firstValue("Content-Type").ifPresent(contentType -> out.append(" ").append(contentType.split(";")[0]));
            response.headers().firstValue("Location").ifPresent(location -> out.append(" Location: ").append(location));
            response.headers().firstValue("Allow").ifPresent(allow -> out.append(" Allow: ").append(String.join(", ", new java.util.TreeSet<>(List.of(allow.split(", "))))));
            String text = response.body().replaceAll("\"instance\":\"[^\"]*\",?", "").replaceAll(",}", "}").replaceAll("\\s+", " ").trim();
            if (!text.isEmpty()) {
                out.append(" | ").append(text);
            }
            System.out.println(out);
            lines.add(out.toString());
        }

        private HttpResponse<String> request(String method, String path, String type, String body) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            if (type != null && type.startsWith("Accept: ")) {
                builder.header("Accept", type.substring("Accept: ".length()));
            } else if (type != null) {
                builder.header("Content-Type", type);
            }
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
