package dev.canhdominich.boot.security;

import dev.canhdominich.boot.broken.BrokenApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ConfiguredSecurityDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(SecurityApplication.class);
        application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "OFF"));
        try (ConfigurableApplicationContext context = application.run()) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            String base = "http://127.0.0.1:" + port;
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            lines.add("-- who may call what (rows: caller, columns: endpoint)");
            lines.add(row(http, base, null, "no credentials"));
            lines.add(row(http, base, "an:an-secret", "an (USER)"));
            lines.add(row(http, base, "admin:admin-secret", "admin (ADMIN, USER)"));
            lines.add(row(http, base, "an:wrong", "an, wrong password"));
            lines.add("-- method security: owner check on /api/accounts/{owner}");
            lines.add("an reads accounts/an -> " + status(http, base + "/api/accounts/an", "an:an-secret", "GET"));
            lines.add("an reads accounts/binh -> " + status(http, base + "/api/accounts/binh", "an:an-secret", "GET"));
            lines.add("admin reads accounts/binh -> " + status(http, base + "/api/accounts/binh", "admin:admin-secret", "GET"));
            lines.add("-- CSRF ignored for /api only");
            lines.add("POST /api/orders with Basic, no token -> " + status(http, base + "/api/orders", "an:an-secret", "POST"));
            lines.add("-- how passwords are stored");
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            String stored = jdbc.queryForObject("select password from users where username = 'an'", String.class);
            lines.add("stored value starts with: " + stored.substring(0, 11) + " (length " + stored.length() + ")");
            PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
            String first = encoder.encode("same-password");
            String second = encoder.encode("same-password");
            lines.add("same password encoded twice gives different values: " + !first.equals(second) + "; both match: " + (encoder.matches("same-password", first) && encoder.matches("same-password", second)));
            lines.add("wrong password matches: " + encoder.matches("other", first));
            lines.add("explicit {noop} prefix still matches: " + encoder.matches("plain", "{noop}plain"));
            try {
                encoder.matches("plain", "plain");
            } catch (IllegalArgumentException e) {
                lines.add("stored without a prefix -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            lines.add("-- cost of one BCrypt hash");
            for (int strength : new int[] {4, 10, 12, 14}) {
                BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(strength);
                long start = System.nanoTime();
                bcrypt.encode("a-password");
                long millis = (System.nanoTime() - start) / 1_000_000;
                lines.add("strength " + strength + ": " + millis + " ms");
            }
            lines.add("-- login timing: unknown user against known user with a wrong password");
            long unknown = averageMillis(http, base, "ghost:whatever");
            long known = averageMillis(http, base, "an:wrong");
            lines.add("unknown user: " + unknown + " ms, known user with a wrong password: " + known + " ms, similar: " + (Math.abs(unknown - known) < Math.max(unknown, known) / 2 + 20));
        }
        lines.add("-- a misplaced rule is rejected at startup");
        lines.add(broken());
        lines.forEach(System.out::println);
        return lines;
    }

    private static long averageMillis(HttpClient http, String base, String credentials) throws Exception {
        long total = 0;
        int runs = 5;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            status(http, base + "/api/me", credentials, "GET");
            total += (System.nanoTime() - start) / 1_000_000;
        }
        return total / runs;
    }

    private static String broken() {
        SpringApplication application = new SpringApplication(BrokenApplication.class);
        application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "OFF"));
        try (ConfigurableApplicationContext context = application.run()) {
            return "started normally";
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            return "startup failed: " + root.getClass().getSimpleName() + ": " + root.getMessage().split("\n")[0];
        }
    }

    private static String row(HttpClient http, String base, String credentials, String label) throws Exception {
        return String.format("%-22s /public/info %s | /api/me %s | /admin/stats %s", label,
                status(http, base + "/public/info", credentials, "GET"), status(http, base + "/api/me", credentials, "GET"), status(http, base + "/admin/stats", credentials, "GET"));
    }

    private static String status(HttpClient http, String url, String credentials, String method) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).method(method, HttpRequest.BodyPublishers.noBody());
        if (credentials != null) {
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return String.valueOf(response.statusCode());
    }
}
