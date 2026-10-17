package dev.canhdominich.boot.security;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.FilterChainProxy;

public class DefaultSecurityDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        SpringApplication application = new SpringApplication(SecurityApplication.class);
        application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "INFO"));
        ConfigurableApplicationContext context;
        System.setOut(new PrintStream(captured, true));
        try {
            context = application.run();
        } finally {
            System.setOut(original);
        }
        try (context) {
            Matcher matcher = Pattern.compile("Using generated security password: (\\S+)").matcher(captured.toString());
            String password = matcher.find() ? matcher.group(1) : "";
            lines.add("generated password logged at startup: " + (!password.isEmpty()) + " (a random UUID, " + password.length() + " characters)");
            lines.add("filters in the default chain: " + filterNames(context));
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            String base = "http://127.0.0.1:" + port;
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            String basic = "Basic " + Base64.getEncoder().encodeToString(("user:" + password).getBytes());
            String wrong = "Basic " + Base64.getEncoder().encodeToString("user:wrong".getBytes());
            lines.add("-- who gets in");
            lines.add(send(http, "GET", base + "/hello", null, null));
            lines.add(send(http, "GET", base + "/hello", null, "text/html"));
            lines.add(send(http, "GET", base + "/hello", wrong, null));
            lines.add(send(http, "GET", base + "/hello", basic, null));
            lines.add(send(http, "GET", base + "/no-such-page", null, null));
            lines.add(send(http, "GET", base + "/no-such-page", basic, null));
            lines.add("-- headers added to an authenticated answer");
            HttpResponse<String> ok = http.send(HttpRequest.newBuilder(URI.create(base + "/hello")).header("Authorization", basic).GET().build(), HttpResponse.BodyHandlers.ofString());
            lines.add("response headers: " + new TreeSet<>(ok.headers().map().keySet().stream().filter(name -> !name.equalsIgnoreCase("date") && !name.equalsIgnoreCase("content-length") && !name.equalsIgnoreCase("keep-alive") && !name.equalsIgnoreCase("connection")).toList()));
            lines.add("-- CSRF protection on POST");
            lines.add(send(http, "POST", base + "/orders", basic, null));
            lines.add("-- the default login page and form login");
            HttpResponse<String> loginPage = http.send(HttpRequest.newBuilder(URI.create(base + "/login")).header("Accept", "text/html").GET().build(), HttpResponse.BodyHandlers.ofString());
            String cookie = loginPage.headers().firstValue("Set-Cookie").orElse("");
            Matcher token = Pattern.compile("name=\"_csrf\" type=\"hidden\" value=\"([^\"]+)\"").matcher(loginPage.body());
            String csrf = token.find() ? token.group(1) : "";
            lines.add("GET /login -> " + loginPage.statusCode() + ", form has username, password and _csrf fields: "
                    + (loginPage.body().contains("name=\"username\"") && loginPage.body().contains("name=\"password\"") && !csrf.isEmpty())
                    + ", session cookie attributes: " + cookieAttributes(cookie));
            String sessionCookie = cookie.split(";")[0];
            HttpResponse<String> badLogin = http.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded").header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.ofString("username=user&password=wrong&_csrf=" + csrf)).build(), HttpResponse.BodyHandlers.ofString());
            lines.add("POST /login with a wrong password -> " + badLogin.statusCode() + ", Location " + path(badLogin.headers().firstValue("Location").orElse("")));
            HttpResponse<String> login = http.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded").header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.ofString("username=user&password=" + password + "&_csrf=" + csrf)).build(), HttpResponse.BodyHandlers.ofString());
            String newCookie = login.headers().firstValue("Set-Cookie").map(c -> c.split(";")[0]).orElse(sessionCookie);
            lines.add("POST /login with the right password and the token -> " + login.statusCode() + ", Location " + path(login.headers().firstValue("Location").orElse("")) + ", session id changed: " + !newCookie.equals(sessionCookie));
            HttpResponse<String> afterLogin = http.send(HttpRequest.newBuilder(URI.create(base + "/hello")).header("Cookie", newCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            lines.add("GET /hello with only the session cookie -> " + afterLogin.statusCode() + " " + afterLogin.body());
            HttpResponse<String> postWithSession = http.send(HttpRequest.newBuilder(URI.create(base + "/orders")).header("Cookie", newCookie).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            lines.add("POST /orders with the session cookie and no CSRF token -> " + postWithSession.statusCode());
        }
        lines.forEach(System.out::println);
        return lines;
    }

    private static String filterNames(ConfigurableApplicationContext context) {
        FilterChainProxy proxy = context.getBean(FilterChainProxy.class);
        List<String> names = new ArrayList<>();
        proxy.getFilterChains().get(0).getFilters().forEach(filter -> names.add(filter.getClass().getSimpleName()));
        return names.toString();
    }

    private static String cookieAttributes(String cookie) {
        List<String> attributes = new ArrayList<>();
        for (String part : cookie.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.startsWith("JSESSIONID")) {
                attributes.add(trimmed);
            }
        }
        return attributes.toString();
    }

    private static String path(String location) {
        return location.isEmpty() ? "(none)" : URI.create(location).getPath() + (URI.create(location).getQuery() == null ? "" : "?" + URI.create(location).getQuery());
    }

    private static String send(HttpClient http, String method, String url, String authorization, String accept) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).method(method, HttpRequest.BodyPublishers.noBody());
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (accept != null) {
            builder.header("Accept", accept);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        StringBuilder out = new StringBuilder(method + " " + url.substring(url.indexOf("/", 8)) + (authorization == null ? " (no credentials)" : authorization.equals("") ? "" : (authorization.contains("dXNlcjp3cm9uZw==") ? " (wrong password)" : " (right password)")) + (accept == null ? "" : " Accept " + accept) + " -> " + response.statusCode());
        response.headers().firstValue("WWW-Authenticate").ifPresent(v -> out.append(", WWW-Authenticate: ").append(v));
        response.headers().firstValue("Location").ifPresent(v -> out.append(", Location: ").append(path(v)));
        if (response.statusCode() == 200) {
            out.append(", body ").append(response.body());
        }
        return out.toString();
    }
}
