package dev.canhdominich.servlet.reqresp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class RequestResponseProbes {

    private static final String FORM = "application/x-www-form-urlencoded";

    private RequestResponseProbes() {
    }

    public static void main(String[] args) throws Exception {
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            Probe probe = new Probe("http://127.0.0.1:" + server.port());
            reading(probe);
            encoding(probe);
            body(probe);
            responses(probe);
            sessions(probe);
        }
    }

    static void reading(Probe probe) throws Exception {
        System.out.println("-- reading the request: GET with query string");
        System.out.println(probe.send("GET", "/echo?name=An&hobby=code&hobby=music", null, "User-Agent", "lab-client").body());
        System.out.println("-- POST form: query string and body both carry name");
        System.out.println(probe.send("POST", "/echo?name=fromQuery", "name=fromForm&hobby=a", "Content-Type", FORM).body());
    }

    static void encoding(Probe probe) throws Exception {
        System.out.println("-- response without and with charset");
        for (String path : new String[]{"/encoding/response-default", "/encoding/response-utf8"}) {
            HttpResponse<byte[]> response = probe.sendBytes("GET", path);
            System.out.println(path + " Content-Type=" + response.headers().firstValue("Content-Type").orElse("none")
                    + " as UTF-8: " + new String(response.body(), StandardCharsets.UTF_8).trim()
                    + " | as ISO-8859-1: " + new String(response.body(), StandardCharsets.ISO_8859_1).trim());
        }
        System.out.println("-- request body encoding, form field ten=Nguyễn sent as UTF-8");
        String form = "ten=Nguy%E1%BB%85n";
        System.out.println("no setCharacterEncoding        : " + oneLine(probe.send("POST", "/encoding/request", form, "Content-Type", FORM).body()));
        System.out.println("setCharacterEncoding(UTF-8) first : " + oneLine(probe.send("POST", "/encoding/request", form, "Content-Type", FORM, "X-Force-Charset", "UTF-8").body()));
        System.out.println("setCharacterEncoding(ISO-8859-1) first : " + oneLine(probe.send("POST", "/encoding/request", form, "Content-Type", FORM, "X-Force-Charset", "ISO-8859-1").body()));
        System.out.println("setCharacterEncoding(ISO-8859-1) after getParameter : " + oneLine(probe.send("POST", "/encoding/request", form, "Content-Type", FORM, "X-Force-Charset", "ISO-8859-1", "X-Order", "late").body()));
    }

    static void body(Probe probe) throws Exception {
        System.out.println("-- the request body is a one-shot stream");
        System.out.println("read body, then getParameter : " + oneLine(probe.send("POST", "/body/read-first", "ten=An", "Content-Type", FORM).body()));
        System.out.println("getParameter, then read body : " + oneLine(probe.send("POST", "/body/parameter-first", "ten=An", "Content-Type", FORM).body()));
        HttpResponse<String> json = probe.send("POST", "/body/json", "{\"sku\":\"KB-01\"}", "Content-Type", "application/json");
        System.out.println("JSON body                    : " + json.statusCode() + " " + json.body());
    }

    static void responses(Probe probe) throws Exception {
        System.out.println("-- status, error, redirect, committed response, cookie");
        HttpResponse<String> created = probe.send("POST", "/resp/status", "", "Content-Type", FORM);
        System.out.println("setStatus(201)   : " + created.statusCode() + " Location=" + created.headers().firstValue("Location").orElse("none"));
        HttpResponse<String> error = probe.send("GET", "/resp/error", null);
        System.out.println("sendError(404)   : " + error.statusCode() + " body mentions the message: " + error.body().contains("no such order") + ", mentions Apache Tomcat: " + error.body().contains("Apache Tomcat"));
        HttpResponse<String> redirect = probe.send("POST", "/resp/redirect", "", "Content-Type", FORM);
        System.out.println("sendRedirect     : " + redirect.statusCode() + " Location=" + redirect.headers().firstValue("Location").orElse("none"));
        System.out.println("response committed:\n" + probe.send("GET", "/resp/committed", null).body().trim());
        HttpResponse<String> cookie = probe.send("GET", "/resp/cookie", null);
        System.out.println("Set-Cookie       : " + cookie.headers().firstValue("Set-Cookie").orElse("none"));
        System.out.println("sending it back  : " + probe.send("GET", "/resp/cookie", null, "Cookie", "theme=dark").body().trim());
    }

    static void sessions(Probe probe) throws Exception {
        System.out.println("-- sessions");
        HttpResponse<String> first = probe.send("GET", "/session/start", null);
        String setCookie = first.headers().firstValue("Set-Cookie").orElse("none");
        System.out.println("first request   : " + first.body().trim().replaceAll("id=\\w+", "id=<id>"));
        System.out.println("Set-Cookie      : " + setCookie.replaceAll("JSESSIONID=\\w+", "JSESSIONID=<id>"));
        String id = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        HttpResponse<String> second = probe.send("GET", "/session/start", null, "Cookie", "JSESSIONID=" + id);
        System.out.println("with the cookie : " + second.body().trim().replaceAll("id=\\w+", "id=<id>") + " Set-Cookie sent again: " + second.headers().firstValue("Set-Cookie").isPresent());
        System.out.println("whoami, no cookie : " + probe.send("GET", "/session/whoami", null).body().trim());

        System.out.println("-- session fixation: the attacker plants a session id, the victim logs in");
        for (boolean rotate : new boolean[]{false, true}) {
            String planted = probe.sessionId(probe.send("GET", "/session/start", null));
            HttpResponse<String> login = probe.send("POST", "/session/login" + (rotate ? "?rotate=1" : ""), "user=victim", "Content-Type", FORM, "Cookie", "JSESSIONID=" + planted);
            String seenByAttacker = probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + planted).body().trim();
            System.out.println((rotate ? "changeSessionId() on login : " : "no id change on login    : ") + "id changed=" + !login.body().contains("idBefore=" + planted + " idAfter=" + planted) + ", attacker using the planted id sees: " + seenByAttacker);
        }

        System.out.println("-- logout and timeout");
        String logged = probe.sessionId(probe.send("GET", "/session/start", null));
        probe.send("POST", "/session/login", "user=an", "Content-Type", FORM, "Cookie", "JSESSIONID=" + logged);
        System.out.println("after login  : " + probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + logged).body().trim());
        probe.send("POST", "/session/logout", "", "Content-Type", FORM, "Cookie", "JSESSIONID=" + logged);
        System.out.println("after logout : " + probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + logged).body().trim());
        String shortLived = probe.sessionId(probe.send("GET", "/session/start", null));
        probe.send("POST", "/session/login?timeout=2", "user=an", "Content-Type", FORM, "Cookie", "JSESSIONID=" + shortLived);
        System.out.println("timeout 2 s, immediately : " + probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + shortLived).body().trim());
        Thread.sleep(3500);
        System.out.println("timeout 2 s, after 3.5 s: " + probe.send("GET", "/session/whoami", null, "Cookie", "JSESSIONID=" + shortLived).body().trim());
    }

    private static String oneLine(String text) {
        return text.trim().replace("\n", " | ");
    }

    static final class Probe {

        private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        private final String base;

        Probe(String base) {
            this.base = base;
        }

        HttpResponse<String> send(String method, String path, String body, String... headers) throws Exception {
            return client.send(request(method, path, body, headers), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        HttpResponse<byte[]> sendBytes(String method, String path) throws Exception {
            return client.send(request(method, path, null), HttpResponse.BodyHandlers.ofByteArray());
        }

        String sessionId(HttpResponse<String> response) {
            String header = response.headers().firstValue("Set-Cookie").orElseThrow();
            return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
        }

        private HttpRequest request(String method, String path, String body, String... headers) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
            for (int i = 0; i + 1 < headers.length; i += 2) {
                builder.header(headers[i], headers[i + 1]);
            }
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            return builder.build();
        }
    }
}
