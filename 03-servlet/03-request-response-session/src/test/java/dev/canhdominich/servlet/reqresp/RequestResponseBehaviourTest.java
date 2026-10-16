package dev.canhdominich.servlet.reqresp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestResponseBehaviourTest {

    private static final String FORM = "application/x-www-form-urlencoded";
    private static EmbeddedServer server;
    private static RequestResponseProbes.Probe probe;

    @BeforeAll
    static void start() throws Exception {
        server = EmbeddedServer.start(0);
        probe = new RequestResponseProbes.Probe("http://127.0.0.1:" + server.port());
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    @Test
    void queryStringWinsOverFormBodyForTheSameParameterName() throws Exception {
        String body = probe.send("POST", "/echo?name=fromQuery", "name=fromForm&hobby=a", "Content-Type", FORM).body();
        assertThat(body).contains("name          = fromQuery").contains("hobby         = [a]");
        String get = probe.send("GET", "/echo?name=An&hobby=code&hobby=music", null, "User-Agent", "lab-client").body();
        assertThat(get).contains("hobby         = [code, music]").contains("missing       = null").contains("User-Agent    = lab-client");
    }

    @Test
    void responseWithoutCharsetIsEncodedAsLatin1AndLosesCharacters() throws Exception {
        HttpResponse<byte[]> plain = probe.sendBytes("GET", "/encoding/response-default");
        assertThat(plain.headers().firstValue("Content-Type")).isEmpty();
        assertThat(new String(plain.body(), StandardCharsets.ISO_8859_1)).contains("Xin chào Vi?t Nam");
        HttpResponse<byte[]> utf8 = probe.sendBytes("GET", "/encoding/response-utf8");
        assertThat(new String(utf8.body(), StandardCharsets.UTF_8)).contains("Xin chào Việt Nam");
    }

    @Test
    void requestBodyIsDecodedAsLatin1UnlessCharacterEncodingIsSetBeforeFirstParameterRead() throws Exception {
        String form = "ten=Nguy%E1%BB%85n";
        String plain = probe.send("POST", "/encoding/request", form, "Content-Type", FORM).body();
        assertThat(plain).contains("ten = Nguyá»").doesNotContain("Nguyễn");
        assertThat(probe.send("POST", "/encoding/request", form, "Content-Type", FORM, "X-Force-Charset", "UTF-8").body()).contains("ten = Nguyễn");
        String late = probe.send("POST", "/encoding/request", form, "Content-Type", FORM, "X-Force-Charset", "UTF-8", "X-Order", "late").body();
        assertThat(late).contains("declared encoding = UTF-8").contains("ten = Nguyá»").doesNotContain("Nguyễn");
    }

    @Test
    void bodyAndParametersConsumeTheSameOneShotStream() throws Exception {
        assertThat(probe.send("POST", "/body/read-first", "ten=An", "Content-Type", FORM).body()).contains("ten after reading the body = null");
        assertThat(probe.send("POST", "/body/parameter-first", "ten=An", "Content-Type", FORM).body()).contains("ten = An").contains("body length after getParameter = 0");
        HttpResponse<String> json = probe.send("POST", "/body/json", "{\"sku\":\"KB-01\"}", "Content-Type", "application/json");
        assertThat(json.statusCode()).isEqualTo(201);
        assertThat(json.body()).contains("\"received\":15");
    }

    @Test
    void statusRedirectErrorAndCommittedResponse() throws Exception {
        HttpResponse<String> created = probe.send("POST", "/resp/status", "", "Content-Type", FORM);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location")).contains("/orders/42");
        HttpResponse<String> redirect = probe.send("POST", "/resp/redirect", "", "Content-Type", FORM);
        assertThat(redirect.statusCode()).isEqualTo(302);
        assertThat(redirect.headers().firstValue("Location")).contains("/echo?name=after-redirect");
        assertThat(probe.send("GET", "/resp/error", null).statusCode()).isEqualTo(404);
        String committed = probe.send("GET", "/resp/committed", null).body();
        assertThat(committed).contains("bufferSize=8192").contains("committedBefore=false").contains("IllegalStateException: Cannot call sendRedirect() after the response has been committed");
    }

    @Test
    void cookieAttributesAreWrittenIntoSetCookie() throws Exception {
        String header = probe.send("GET", "/resp/cookie", null).headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(header).contains("theme=dark").contains("Max-Age=604800").contains("Path=/").contains("Secure").contains("HttpOnly").contains("SameSite=Lax");
        assertThat(probe.send("GET", "/resp/cookie", null, "Cookie", "theme=dark").body()).contains("cookies received: theme=dark");
    }

    @Test
    void sessionIsCreatedOnDemandWithHttpOnlyCookieAndDefaultThirtyMinutes() throws Exception {
        assertThat(probe.send("GET", "/session/whoami", null).body()).contains("anonymous (no session)");
        HttpResponse<String> first = probe.send("GET", "/session/start", null);
        assertThat(first.body()).contains("new=true").contains("maxInactiveInterval=1800");
        String setCookie = first.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(setCookie).startsWith("JSESSIONID=").contains("Path=/").contains("HttpOnly").doesNotContain("Secure");
        HttpResponse<String> second = probe.send("GET", "/session/start", null, "Cookie", "JSESSIONID=" + probe.sessionId(first));
        assertThat(second.body()).contains("new=false");
        assertThat(second.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void changingTheSessionIdOnLoginDefeatsFixation() throws Exception {
        String planted = probe.sessionId(probe.send("GET", "/session/start", null));
        probe.send("POST", "/session/login", "user=victim", "Content-Type", FORM, "Cookie", "JSESSIONID=" + planted);
        assertThat(probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + planted).body()).contains("user=victim");

        String planted2 = probe.sessionId(probe.send("GET", "/session/start", null));
        probe.send("POST", "/session/login?rotate=1", "user=victim", "Content-Type", FORM, "Cookie", "JSESSIONID=" + planted2);
        assertThat(probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + planted2).body()).contains("anonymous");
    }

    @Test
    void logoutAndTimeoutEndTheSession() throws Exception {
        String id = probe.sessionId(probe.send("GET", "/session/start", null));
        probe.send("POST", "/session/login", "user=an", "Content-Type", FORM, "Cookie", "JSESSIONID=" + id);
        assertThat(probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + id).body()).contains("user=an");
        probe.send("POST", "/session/logout", "", "Content-Type", FORM, "Cookie", "JSESSIONID=" + id);
        assertThat(probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + id).body()).contains("anonymous");

        String shortLived = probe.sessionId(probe.send("GET", "/session/start", null));
        probe.send("POST", "/session/login?timeout=1", "user=an", "Content-Type", FORM, "Cookie", "JSESSIONID=" + shortLived);
        Thread.sleep(2500);
        assertThat(probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + shortLived).body()).contains("anonymous");
    }
}
