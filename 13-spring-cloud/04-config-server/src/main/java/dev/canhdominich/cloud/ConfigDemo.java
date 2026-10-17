package dev.canhdominich.cloud;

import dev.canhdominich.cloud.client.OrdersApplication;
import dev.canhdominich.cloud.server.ConfigServerApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class ConfigDemo {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        Path repo = Files.createTempDirectory("config-repo");
        Files.writeString(repo.resolve("application.yml"), "orders:\n  currency: USD\n  region: shared\n");
        Files.writeString(repo.resolve("orders.yml"), "orders:\n  max-items: 10\n  currency: VND\n");
        Files.writeString(repo.resolve("orders-prod.yml"), "orders:\n  max-items: 50\n");
        ConfigurableApplicationContext server = new SpringApplicationBuilder(ConfigServerApplication.class)
                .properties("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF",
                        "spring.profiles.active=native",
                        "spring.cloud.config.server.native.search-locations=file:" + repo.toAbsolutePath(),
                        "encrypt.key=lab-only-secret-key")
                .run();
        int serverPort = port(server);
        String configUrl = "http://localhost:" + serverPort;
        try {
            lines.add("-- what the server serves");
            lines.add("GET /orders/default property sources: " + sources(get(serverPort, "/orders/default")));
            lines.add("GET /orders/prod property sources: " + sources(get(serverPort, "/orders/prod")));
            lines.add("-- a client reads its configuration at startup");
            try (ConfigurableApplicationContext client = client(configUrl, "default")) {
                int p = port(client);
                lines.add("profile default: " + get(p, "/policy"));
                lines.add("-- the repository changes");
                Files.writeString(repo.resolve("orders.yml"), "orders:\n  max-items: 25\n  currency: VND\n");
                lines.add("server now serves max-items: " + value(get(serverPort, "/orders/default"), "orders.max-items"));
                lines.add("client before refresh: " + get(p, "/policy"));
                lines.add("POST /actuator/refresh returns: " + sortedKeys(post(p, "/actuator/refresh")));
                lines.add("client after refresh: " + get(p, "/policy"));
                lines.add("bean without @RefreshScope after refresh: " + get(p, "/static-policy"));
            }
            Files.writeString(repo.resolve("orders.yml"), "orders:\n  max-items: 10\n  currency: VND\n");
            try (ConfigurableApplicationContext client = client(configUrl, "prod")) {
                lines.add("profile prod: " + get(port(client), "/policy"));
            }
            lines.add("-- local settings against the server");
            try (ConfigurableApplicationContext client = client(configUrl, "default", false, new String[0], "orders.max-items=99")) {
                lines.add("local default property orders.max-items=99: " + get(port(client), "/policy"));
            }
            try (ConfigurableApplicationContext client = client(configUrl, "default", false, new String[] {"--orders.max-items=99"})) {
                lines.add("command-line argument --orders.max-items=99: " + get(port(client), "/policy"));
            }
            lines.add("-- secrets");
            String cipher = post(serverPort, "/encrypt", "s3cret-value");
            lines.add("encrypted value starts with a hex string: " + cipher.matches("[0-9a-f]+"));
            Files.writeString(repo.resolve("orders.yml"), "orders:\n  max-items: 10\n  currency: VND\n  api-token: '{cipher}" + cipher + "'\n");
            lines.add("server answers with the decrypted value: " + value(get(serverPort, "/orders/default"), "orders.api-token"));
            lines.add("-- the server is not reachable at startup");
            server.close();
            try (ConfigurableApplicationContext client = client(configUrl, "default")) {
                lines.add("optional import: started with " + get(port(client), "/policy"));
            }
            try (ConfigurableApplicationContext client = client(configUrl, "default", true, new String[0])) {
                lines.add("required import: started");
            } catch (Exception e) {
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                lines.add("required import: failed to start, " + e.getClass().getSimpleName());
            }
        } finally {
            server.close();
        }
        return lines;
    }

    private static ConfigurableApplicationContext client(String configUrl, String profile) {
        return client(configUrl, profile, false, new String[0]);
    }

    private static ConfigurableApplicationContext client(String configUrl, String profile, boolean required, String[] args, String... defaults) {
        List<String> properties = new ArrayList<>(List.of("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF",
                "management.endpoints.web.exposure.include=refresh"));
        properties.addAll(List.of(defaults));
        List<String> arguments = new ArrayList<>(List.of(
                "--spring.application.name=orders",
                "--spring.cloud.config.enabled=true",
                "--spring.profiles.active=" + profile,
                "--spring.config.import=" + (required ? "" : "optional:") + "configserver:" + configUrl));
        arguments.addAll(List.of(args));
        return new SpringApplicationBuilder(OrdersApplication.class).properties(properties.toArray(String[]::new)).run(arguments.toArray(String[]::new));
    }

    private static int port(ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private static String get(int port, String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String post(int port, String path) throws Exception {
        return post(port, path, "{}", "application/json");
    }

    private static String post(int port, String path, String body) throws Exception {
        return post(port, path, body, "text/plain");
    }

    private static String post(int port, String path, String body, String contentType) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String sources(String json) {
        List<String> names = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"name\":\"[^\"]*?([A-Za-z0-9\\-]+\\.yml)\"").matcher(json);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names.toString();
    }

    private static String value(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\":\\s*\"?([^\",}]+)").matcher(json);
        return matcher.find() ? matcher.group(1) : "absent";
    }

    private static String sortedKeys(String jsonArray) {
        List<String> keys = new ArrayList<>(List.of(jsonArray.replaceAll("[\\[\\]\"\\s]", "").split(",")));
        keys.sort(String::compareTo);
        return keys.toString();
    }
}
