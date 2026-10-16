package dev.canhdominich.jsp.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MvcBehaviourTest {

    private static EmbeddedServer server;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        server = EmbeddedServer.start(0);
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    private static HttpClient browser(CookieManager cookies) {
        return HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private static String token(HttpClient client) throws Exception {
        return MvcProbes.extract(MvcProbes.get(client, base + "/login").body(), "name=\"csrf\" value=\"([^\"]*)\"");
    }

    @Test
    void protectedPageRedirectsAnonymousVisitorsToLogin() throws Exception {
        HttpResponse<String> response = MvcProbes.get(browser(new CookieManager()), base + "/dashboard");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).contains("/login?next=%2Fdashboard");
    }

    @Test
    void viewsUnderWebInfCannotBeRequestedDirectly() throws Exception {
        assertThat(MvcProbes.get(browser(new CookieManager()), base + "/WEB-INF/views/login.jsp").statusCode()).isEqualTo(404);
    }

    @Test
    void postWithoutValidCsrfTokenIsRejected() throws Exception {
        HttpClient client = browser(new CookieManager());
        token(client);
        assertThat(MvcProbes.post(client, base + "/login", "username=an&password=secret123").statusCode()).isEqualTo(403);
        assertThat(MvcProbes.post(client, base + "/login", "csrf=forged&username=an&password=secret123").statusCode()).isEqualTo(403);
    }

    @Test
    void wrongPasswordRedisplaysTheFormWith401AndEscapesTheUsername() throws Exception {
        HttpClient client = browser(new CookieManager());
        String csrf = token(client);
        HttpResponse<String> response = MvcProbes.post(client, base + "/login", "csrf=" + csrf + "&username=" + URLEncoder.encode("an<b>", StandardCharsets.UTF_8) + "&password=wrong");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("Sai tên đăng nhập hoặc mật khẩu").contains("value=\"an&lt;b&gt;\"").doesNotContain("an<b>");
    }

    @Test
    void successfulLoginChangesSessionIdRedirectsAndLogoutEndsTheSession() throws Exception {
        CookieManager cookies = new CookieManager();
        HttpClient client = browser(cookies);
        String csrf = token(client);
        String before = MvcProbes.sessionId(cookies, server);
        HttpResponse<String> login = MvcProbes.post(client, base + "/login", "csrf=" + csrf + "&username=an&password=secret123");
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location")).contains("/dashboard");
        assertThat(MvcProbes.sessionId(cookies, server)).isNotEqualTo(before);
        assertThat(MvcProbes.get(client, base + "/dashboard").body()).contains("Xin chào, Nguyễn Văn An");
        assertThat(MvcProbes.post(client, base + "/logout", "").statusCode()).isEqualTo(302);
        assertThat(MvcProbes.get(client, base + "/dashboard").statusCode()).isEqualTo(302);
    }

    @Test
    void openRedirectTargetsAreIgnored() throws Exception {
        HttpClient client = browser(new CookieManager());
        String csrf = token(client);
        HttpResponse<String> login = MvcProbes.post(client, base + "/login", "csrf=" + csrf + "&username=an&password=secret123&next=" + URLEncoder.encode("//evil.example/steal", StandardCharsets.UTF_8));
        assertThat(login.headers().firstValue("Location")).contains("/dashboard");
    }

    @Test
    void passwordsAreStoredAsSaltedPbkdf2AndMatchOnlyTheRightPassword() {
        String stored = PasswordHasher.hash("secret123");
        assertThat(stored).startsWith("600000:");
        assertThat(PasswordHasher.hash("secret123")).isNotEqualTo(stored);
        assertThat(PasswordHasher.matches("secret123", stored)).isTrue();
        assertThat(PasswordHasher.matches("secret124", stored)).isFalse();
    }

    @Test
    void unknownUserCostsAboutAsMuchAsAWrongPasswordOnlyInTheHardenedVersion() {
        UserDao users = new UserDao();
        users.authenticate("an", "warm-up");
        double wrong = MvcProbes.average(() -> users.authenticate("an", "wrong"), 3);
        double unknown = MvcProbes.average(() -> users.authenticate("nobody", "wrong"), 3);
        assertThat(unknown).isGreaterThan(wrong * 0.5);
        double naiveUnknown = MvcProbes.average(() -> users.authenticateNaive("nobody", "wrong"), 3);
        assertThat(naiveUnknown * 20).isLessThan(wrong);
    }
}
