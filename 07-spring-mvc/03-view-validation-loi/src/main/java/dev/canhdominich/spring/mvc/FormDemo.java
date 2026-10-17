package dev.canhdominich.spring.mvc;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        try (EmbeddedServer server = EmbeddedServer.start(0, WebConfig.class)) {
            server.springContext().getBean(UserStore.class).clear();
            Client client = new Client(server.port(), lines);
            client.get("/register");
            client.post("/register", "name=&email=x&age=10");
            client.post("/register", "name=An&email=an%40example.com&age=20");
            client.get("/registered");
            client.get("/registered");
            client.post("/register-no-redirect", "name=Binh&email=binh%40example.com&age=30");
            client.post("/register-no-redirect", "name=Binh&email=binh%40example.com&age=30");
            client.post("/register-strict", "name=&email=x&age=10");
            client.get("/pages/course/1");
            client.get("/pages/course/9");
            client.get("/local/course/9");
            client.get("/api/course/9");
            client.postJson("/api/users", "{\"name\":\"\",\"email\":\"x\",\"age\":10}");
            client.get("/api/boom");
        }
        return lines;
    }

    static final class Client {

        private final int port;
        private final List<String> lines;
        private final HttpClient http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        Client(int port, List<String> lines) {
            this.port = port;
            this.lines = lines;
        }

        void get(String path) throws Exception {
            send("GET " + path, HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build());
        }

        void post(String path, String form) throws Exception {
            send("POST " + path + " " + form, HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build());
        }

        void postJson(String path, String json) throws Exception {
            send("POST " + path + " " + json, HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build());
        }

        private void send(String label, HttpRequest request) throws Exception {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            StringBuilder out = new StringBuilder(label + " -> " + response.statusCode());
            response.headers().firstValue("Location").ifPresent(location -> out.append(" Location: ").append(path(location)));
            String body = response.body();
            List<String> errors = new ArrayList<>();
            Matcher matcher = Pattern.compile("id=\"([a-z]+)\\.errors\"[^>]*>([^<]*)<").matcher(body);
            while (matcher.find()) {
                errors.add(matcher.group(1) + ": " + matcher.group(2));
            }
            if (!errors.isEmpty()) {
                out.append(" | errors ").append(errors);
            }
            Matcher message = Pattern.compile("id=\"(message|count|error)\">([^<]*)<").matcher(body);
            StringBuilder page = new StringBuilder();
            while (message.find()) {
                page.append(message.group(1)).append("=[").append(message.group(2)).append("] ");
            }
            if (page.length() > 0) {
                out.append(" | ").append(page.toString().trim());
            }
            String type = response.headers().firstValue("Content-Type").orElse("");
            if (response.statusCode() < 400 && body.length() < 60 && !body.isBlank() && errors.isEmpty() && page.length() == 0 || type.contains("json") || (type.contains("text/plain") && !body.isBlank())) {
                out.append(" | ").append(body.replaceAll("\\s+", " ").trim());
            }
            if (body.contains("<form") && errors.isEmpty()) {
                out.append(" | form with inputs for name, email, age");
            }
            System.out.println(out);
            lines.add(out.toString());
        }

        private static String path(String location) {
            return URI.create(location).getPath();
        }
    }
}
