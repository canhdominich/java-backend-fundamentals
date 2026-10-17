package dev.canhdominich.boot.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

public class JwtDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(JwtApplication.class);
        application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "OFF"));
        try (ConfigurableApplicationContext context = application.run()) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            String base = "http://127.0.0.1:" + port;
            HttpClient http = HttpClient.newHttpClient();
            TokenService tokens = context.getBean(TokenService.class);
            lines.add("-- logging in");
            String anToken = login(http, base, "an", "an-secret", lines);
            String adminToken = login(http, base, "admin", "admin-secret", lines);
            lines.add("login with a wrong password -> " + call(http, base, "POST", "/auth/login", null, "{\"username\":\"an\",\"password\":\"wrong\"}").summary());
            lines.add("-- what a token contains (anyone can read it)");
            String[] parts = anToken.split("\\.");
            lines.add("parts separated by dots: " + parts.length);
            lines.add("header: " + decode(parts[0]));
            Map<String, Object> claims = claims(parts[1]);
            lines.add("payload claims: " + new java.util.TreeSet<>(claims.keySet()) + ", sub " + claims.get("sub") + ", roles " + claims.get("roles") + ", lifetime " + (((Number) claims.get("exp")).longValue() - ((Number) claims.get("iat")).longValue()) + " s");
            lines.add("-- calling the API");
            lines.add("no token -> " + call(http, base, "GET", "/api/me", null, null).summary());
            lines.add("an token on /api/me -> " + call(http, base, "GET", "/api/me", anToken, null).summary());
            lines.add("an token on /admin/stats -> " + call(http, base, "GET", "/admin/stats", anToken, null).summary());
            lines.add("admin token on /admin/stats -> " + call(http, base, "GET", "/admin/stats", adminToken, null).summary());
            Reply me = call(http, base, "GET", "/api/me", anToken, null);
            lines.add("session cookie set by the server: " + me.setCookie());
            lines.add("-- tokens that must be refused");
            String forged = parts[0] + "." + encode("{\"sub\":\"an\",\"roles\":[\"ADMIN\",\"USER\"],\"iat\":" + claims.get("iat") + ",\"exp\":" + claims.get("exp") + "}") + "." + parts[2];
            lines.add("payload changed to ADMIN, old signature -> " + call(http, base, "GET", "/admin/stats", forged, null).summary());
            String none = encode("{\"alg\":\"none\"}") + "." + parts[1] + ".";
            lines.add("alg none, no signature -> " + call(http, base, "GET", "/admin/stats", none, null).summary());
            NimbusJwtEncoder other = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec("another-secret-that-is-also-32-bytes-long!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
            String foreign = foreignToken(other);
            lines.add("signed with another secret -> " + call(http, base, "GET", "/api/me", foreign, null).summary());
            lines.add("garbage -> " + call(http, base, "GET", "/api/me", "not.a.token", null).summary());
            lines.add("-- time");
            List<SimpleGrantedAuthority> user = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            lines.add("expired 2 hours ago -> " + call(http, base, "GET", "/api/me", tokens.issueAt("an", user, Instant.now().minus(Duration.ofHours(2)).minus(Duration.ofMinutes(15)), Duration.ofMinutes(15)), null).summary());
            lines.add("expired 30 seconds ago -> " + call(http, base, "GET", "/api/me", tokens.issueAt("an", user, Instant.now().minusSeconds(30 + 60), Duration.ofSeconds(60)), null).summary());
            lines.add("expired 90 seconds ago -> " + call(http, base, "GET", "/api/me", tokens.issueAt("an", user, Instant.now().minusSeconds(90 + 60), Duration.ofSeconds(60)), null).summary());
            lines.add("-- there is no revoke");
            context.getBean(InMemoryUserDetailsManager.class).deleteUser("an");
            lines.add("login after the user was deleted -> " + call(http, base, "POST", "/auth/login", null, "{\"username\":\"an\",\"password\":\"an-secret\"}").summary());
            lines.add("old token of the deleted user -> " + call(http, base, "GET", "/api/me", anToken, null).summary());
        }
        lines.forEach(System.out::println);
        return lines;
    }

    private static String foreignToken(NimbusJwtEncoder encoder) {
        org.springframework.security.oauth2.jwt.JwtClaimsSet claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .subject("an").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).claim("roles", List.of("USER")).build();
        return encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static String login(HttpClient http, String base, String user, String password, List<String> lines) throws Exception {
        Reply reply = call(http, base, "POST", "/auth/login", null, "{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}");
        String token = reply.body().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        lines.add("login " + user + " -> " + reply.status() + ", token received: " + (token.length() > 50));
        return token;
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> claims(String part) throws Exception {
        return new tools.jackson.databind.json.JsonMapper().readValue(decode(part), Map.class);
    }

    record Reply(int status, String body, String authenticate, String setCookie) {

        String summary() {
            String text = status + "";
            if (authenticate != null) {
                text += ", WWW-Authenticate: " + authenticate.replaceAll("\\d{4}-\\d{2}-\\d{2}T[0-9:.]+Z", "<time>").replaceAll("127\\.0\\.0\\.1:\\d+", "127.0.0.1:<port>");
            }
            if (status == 200 && !body.isEmpty()) {
                text += ", " + body;
            }
            return text.length() > 300 ? text.substring(0, 300) + "..." : text;
        }
    }

    private static Reply call(HttpClient http, String base, String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body(), response.headers().firstValue("WWW-Authenticate").orElse(null), response.headers().firstValue("Set-Cookie").orElse("none"));
    }
}
