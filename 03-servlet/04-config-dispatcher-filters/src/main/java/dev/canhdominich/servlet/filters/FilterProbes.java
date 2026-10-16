package dev.canhdominich.servlet.filters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class FilterProbes {

    private FilterProbes() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("-- starting the server");
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            HttpClient following = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            String base = "http://127.0.0.1:" + server.port();

            System.out.println("-- ServletConfig and ServletContext");
            System.out.print(get(client, base + "/config").body());

            System.out.println("-- servlets registered in web.xml and in code (the annotation way is ConfigServlet above)");
            System.out.print(get(client, base + "/health").body());
            System.out.print(get(client, base + "/programmatic").body());

            System.out.println("-- forward, include and redirect to the same target");
            show("forward ", get(client, base + "/dispatch/forward"));
            show("include ", get(client, base + "/dispatch/include"));
            HttpResponse<String> redirect = get(client, base + "/dispatch/redirect");
            System.out.println("redirect: first response " + redirect.statusCode() + " Location=" + redirect.headers().firstValue("Location").orElse("none"));
            show("redirect followed", get(following, base + "/dispatch/redirect"));

            System.out.println("-- filter chain around a normal request");
            get(client, base + "/annotated");
            System.out.println("-- auth filter without a token, then with one");
            HttpResponse<String> denied = get(client, base + "/secure/data");
            System.out.println("  status " + denied.statusCode());
            HttpResponse<String> allowed = client.send(HttpRequest.newBuilder(URI.create(base + "/secure/data")).header("X-Token", "secret").build(), HttpResponse.BodyHandlers.ofString());
            System.out.println("  status " + allowed.statusCode() + " body " + allowed.body().trim());

            System.out.println("-- dispatcher types seen by the filter mapped to FORWARD and INCLUDE");
            get(client, base + "/dispatch/target");
            get(client, base + "/dispatch/forward");
            get(client, base + "/dispatch/include");

            System.out.println("-- request wrapper that caches the body");
            HttpResponse<String> api = client.send(HttpRequest.newBuilder(URI.create(base + "/api/echo")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"KB-01\"}")).build(), HttpResponse.BodyHandlers.ofString());
            System.out.print(api.body());

            System.out.println("-- front controller on /app/*");
            for (String path : new String[]{"/app/orders", "/app/orders/42", "/app/health", "/app/unknown"}) {
                HttpResponse<String> response = get(client, base + path);
                System.out.println(path + " -> " + response.statusCode() + " " + response.body().trim());
            }

            System.out.println("-- session listener");
            HttpResponse<String> session = get(client, base + "/config?session=1");
            String cookie = session.headers().firstValue("Set-Cookie").orElseThrow();
            client.send(HttpRequest.newBuilder(URI.create(base + "/config?logout=1")).header("Cookie", cookie.substring(0, cookie.indexOf(';'))).build(), HttpResponse.BodyHandlers.ofString());
            System.out.println("-- stopping the server");
        }
    }

    static void show(String label, HttpResponse<String> response) {
        System.out.println(label + ": status " + response.statusCode() + ", header X-Set-By-Target=" + response.headers().firstValue("X-Set-By-Target").orElse("absent"));
        response.body().lines().forEach(line -> System.out.println("    " + line));
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
