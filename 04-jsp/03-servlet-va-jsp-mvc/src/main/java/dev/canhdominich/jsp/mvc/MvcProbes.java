package dev.canhdominich.jsp.mvc;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MvcProbes {

    private MvcProbes() {
    }

    public static void main(String[] args) throws Exception {
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            String base = "http://127.0.0.1:" + server.port();
            CookieManager cookies = new CookieManager();
            HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();

            System.out.println("-- the flow of one browser");
            HttpResponse<String> protectedPage = get(client, base + "/dashboard");
            System.out.println("GET /dashboard without login      -> " + protectedPage.statusCode() + " Location=" + protectedPage.headers().firstValue("Location").orElse("none"));
            HttpResponse<String> form = get(client, base + "/login");
            String csrf = extract(form.body(), "name=\"csrf\" value=\"([^\"]*)\"");
            String sessionBefore = sessionId(cookies, server);
            System.out.println("GET /login                        -> " + form.statusCode() + ", form has a CSRF token of " + csrf.length() + " characters");
            HttpResponse<String> noToken = post(client, base + "/login", "username=an&password=secret123");
            System.out.println("POST /login without token         -> " + noToken.statusCode());
            HttpResponse<String> wrong = post(client, base + "/login", "csrf=" + csrf + "&username=" + URLEncoder.encode("an<b>", StandardCharsets.UTF_8) + "&password=wrong");
            System.out.println("POST /login with a wrong password -> " + wrong.statusCode() + ", message shown: " + wrong.body().contains("Sai tên đăng nhập hoặc mật khẩu")
                    + ", username echoed escaped: " + wrong.body().contains("value=\"an&lt;b&gt;\"") + ", raw tag echoed: " + wrong.body().contains("an<b>"));
            String retryToken = extract(wrong.body(), "name=\"csrf\" value=\"([^\"]*)\"");
            HttpResponse<String> ok = post(client, base + "/login", "csrf=" + retryToken + "&username=an&password=secret123&next=" + URLEncoder.encode("/dashboard", StandardCharsets.UTF_8));
            System.out.println("POST /login with the right one    -> " + ok.statusCode() + " Location=" + ok.headers().firstValue("Location").orElse("none"));
            System.out.println("session id changed by the login   : " + !sessionBefore.equals(sessionId(cookies, server)));
            HttpResponse<String> dashboard = get(client, base + "/dashboard");
            System.out.println("GET /dashboard after login        -> " + dashboard.statusCode() + ", greets: " + dashboard.body().contains("Xin chào, Nguyễn Văn An"));
            System.out.println("GET /WEB-INF/views/dashboard.jsp  -> " + get(client, base + "/WEB-INF/views/dashboard.jsp").statusCode());
            HttpResponse<String> logout = post(client, base + "/logout", "");
            System.out.println("POST /logout                      -> " + logout.statusCode() + " Location=" + logout.headers().firstValue("Location").orElse("none"));
            System.out.println("GET /dashboard after logout       -> " + get(client, base + "/dashboard").statusCode());

            System.out.println("-- password hashing cost and user enumeration by timing");
            UserDao users = new UserDao();
            System.out.println("stored hash format: " + PasswordHasher.hash("x").replaceAll("^(\\d+):.*$", "$1:<salt>:<hash>"));
            users.authenticate("an", "warm-up");
            double hashMs = average(() -> PasswordHasher.hash("secret123"), 5);
            double wrongMs = average(() -> users.authenticate("an", "wrong"), 5);
            double unknownMs = average(() -> users.authenticate("nobody", "wrong"), 5);
            double naiveWrongMs = average(() -> users.authenticateNaive("an", "wrong"), 5);
            double naiveUnknownMs = average(() -> users.authenticateNaive("nobody", "wrong"), 5);
            System.out.printf("one PBKDF2 hash with %d iterations: %.0f ms%n", PasswordHasher.ITERATIONS, hashMs);
            System.out.printf("authenticate, wrong password: %.0f ms, unknown user: %.0f ms%n", wrongMs, unknownMs);
            System.out.printf("naive version, wrong password: %.0f ms, unknown user: %.1f ms%n", naiveWrongMs, naiveUnknownMs);
        }
    }

    interface Action {
        void run();
    }

    static double average(Action action, int rounds) {
        long start = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            action.run();
        }
        return (System.nanoTime() - start) / 1_000_000.0 / rounds;
    }

    static String sessionId(CookieManager cookies, EmbeddedServer server) {
        return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("JSESSIONID")).map(cookie -> cookie.getValue()).findFirst().orElse("none");
    }

    static String extract(String body, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static HttpResponse<String> post(HttpClient client, String url, String form) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
