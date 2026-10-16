package dev.canhdominich.servlet.lifecycle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class LifecycleProbes {

    static final int INCREMENTS = 2000;

    private LifecycleProbes() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("-- starting the server");
        EmbeddedServer server = EmbeddedServer.start(0);
        HttpClient client = HttpClient.newHttpClient();
        String base = "http://127.0.0.1:" + server.port();
        System.out.println("-- server started; requests to the lazy servlet twice, then the eager servlet once");
        get(client, base + "/life/lazy");
        get(client, base + "/life/lazy");
        get(client, base + "/life/eager");
        System.out.println("-- failing init");
        System.out.println("  ordinary exception, request 1: " + status(get(client, base + "/bad-init/ordinary")));
        System.out.println("  ordinary exception, request 2: " + status(get(client, base + "/bad-init/ordinary")) + " " + get(client, base + "/bad-init/ordinary").body().trim());
        System.out.println("  UnavailableException, request 1: " + status(get(client, base + "/bad-init/unavailable")));
        System.out.println("  UnavailableException, request 2: " + status(get(client, base + "/bad-init/unavailable")));
        System.out.println("-- methods the container adds");
        HttpResponse<String> options = client.send(HttpRequest.newBuilder(URI.create(base + "/methods")).method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("  OPTIONS /methods -> " + options.statusCode() + " Allow=" + options.headers().firstValue("Allow").orElse("none"));
        HttpResponse<String> head = client.send(HttpRequest.newBuilder(URI.create(base + "/methods")).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("  HEAD /methods    -> " + head.statusCode() + " Content-Length=" + head.headers().firstValue("Content-Length").orElse("none") + " body length=" + head.body().length());
        System.out.println("-- " + INCREMENTS + " increments from 16 client threads");
        for (String kind : new String[]{"unsafe", "synchronized", "atomic"}) {
            System.out.printf("  %-12s final value %s%n", kind, hammer(client, base, kind));
        }
        System.out.println("-- stopping the server");
        server.close();
    }

    static String hammer(HttpClient client, String base, String kind) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < INCREMENTS; i++) {
            futures.add(pool.submit(() -> get(client, base + "/count/" + kind)));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();
        return get(client, base + "/count/" + kind + "?peek=1").body().trim();
    }

    private static String status(HttpResponse<String> response) {
        return String.valueOf(response.statusCode());
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
