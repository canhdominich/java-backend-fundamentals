package dev.canhdominich.servlet.intro;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import org.apache.coyote.AbstractProtocol;

public final class HttpProbes {

    private HttpProbes() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("shadow")) {
            System.setProperty("lab.shadowDefault", "true");
        }
        if (args.length > 0 && args[0].equals("harden")) {
            System.setProperty("lab.hardenErrors", "true");
        }
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            HttpClient client = HttpClient.newHttpClient();
            String base = "http://127.0.0.1:" + server.port();
            System.out.println("-- URL mapping");
            for (String path : new String[]{"/orders", "/orders/42", "/orders/42/items", "/report.do", "/orders/x.do", "/other", "/index.html"}) {
                System.out.printf("%-18s -> %s%n", path, summary(get(client, base + path)));
            }
            System.out.println("-- methods and errors");
            HttpResponse<String> post = send(client, HttpRequest.newBuilder(URI.create(base + "/hello")).POST(HttpRequest.BodyPublishers.ofString("x=1")).build());
            System.out.println("POST /hello        -> " + post.statusCode() + " Allow=" + post.headers().firstValue("Allow").orElse("none"));
            HttpResponse<String> missing = get(client, base + "/nowhere/page");
            System.out.println("GET /nowhere/page  -> " + summary(missing));
            HttpResponse<String> boom = get(client, base + "/boom");
            System.out.println("GET /boom          -> " + boom.statusCode() + " exception class in body: " + boom.body().contains("IllegalStateException") + ", secret in body: " + boom.body().contains("hunter2") + ", server version in body: " + boom.body().contains("Apache Tomcat") + ", body length " + boom.body().length());
            System.out.println("-- one instance, many threads");
            describeConcurrency(server, client, base);
        }
    }

    static void describeConcurrency(EmbeddedServer server, HttpClient client, String base) throws Exception {
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            futures.add(client.sendAsync(HttpRequest.newBuilder(URI.create(base + "/who")).build(), HttpResponse.BodyHandlers.ofString()));
        }
        Set<String> instances = new TreeSet<>();
        Set<String> threads = new TreeSet<>();
        String last = "";
        for (CompletableFuture<HttpResponse<String>> future : futures) {
            String[] parts = future.get().body().trim().split(",");
            instances.add(parts[0]);
            threads.add(parts[1]);
            last = parts[3];
        }
        AbstractProtocol<?> protocol = (AbstractProtocol<?>) server.tomcat().getConnector().getProtocolHandler();
        System.out.println("40 parallel requests -> distinct servlet instances=" + instances.size() + ", distinct worker threads>1: " + (threads.size() > 1) + ", init() calls=" + last);
        System.out.println("connector maxThreads=" + protocol.getMaxThreads() + " minSpareThreads=" + protocol.getMinSpareThreads());
    }

    static String summary(HttpResponse<String> response) {
        String body = response.body().trim();
        if (body.startsWith("<!doctype html>")) {
            return response.statusCode() + " Tomcat error page, server version in body: " + body.contains("Apache Tomcat") + ", length " + body.length();
        }
        return body.isEmpty() ? response.statusCode() + " empty body" : body;
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return send(client, HttpRequest.newBuilder(URI.create(url)).build());
    }

    private static HttpResponse<String> send(HttpClient client, HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
