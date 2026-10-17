package dev.canhdominich.cloud;

import dev.canhdominich.cloud.catalog.CatalogApplication;
import dev.canhdominich.cloud.registry.RegistryApplication;
import dev.canhdominich.cloud.shop.ShopApplication;
import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscoveryDemo {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        try {
            Node registry = launch(nodes, RegistryApplication.class, "registry",
                    "--eureka.client.register-with-eureka=false",
                    "--eureka.client.fetch-registry=false",
                    "--eureka.server.enable-self-preservation=false",
                    "--eureka.server.eviction-interval-timer-in-ms=1000",
                    "--eureka.server.response-cache-update-interval-ms=1000");
            String eureka = "http://localhost:" + registry.port + "/eureka/";
            lines.add("-- a registry with nothing registered");
            lines.add("catalog instances in the registry: " + instanceIds(registry.port));
            Node catalogA = launch(nodes, CatalogApplication.class, "catalog", catalogArgs(eureka, "catalog-A"));
            Node catalogB = launch(nodes, CatalogApplication.class, "catalog", catalogArgs(eureka, "catalog-B"));
            Node shop = launch(nodes, ShopApplication.class, "shop",
                    "--eureka.client.service-url.defaultZone=" + eureka,
                    "--eureka.client.register-with-eureka=false",
                    "--eureka.client.jersey.enabled=false",
                    "--eureka.client.registry-fetch-interval-seconds=1",
                    "--spring.cloud.loadbalancer.cache.ttl=1s");
            lines.add("-- two instances of one service register themselves");
            lines.add("shop sees 2 instances of catalog within 40 s: " + waitFor(() -> text(shop.port, "/instances").equals("2"), 40_000));
            lines.add("instances listed by the registry: " + instanceIds(registry.port));
            lines.add("-- the load balancer spreads calls");
            lines.add("20 calls to http://catalog/whoami: " + calls(shop.port, 20));
            lines.add("-- an instance leaves cleanly (SIGTERM)");
            catalogA.process.destroy();
            catalogA.process.waitFor();
            long start = System.currentTimeMillis();
            boolean gone = waitFor(() -> instanceIds(registry.port).equals("[catalog-B]"), 15_000);
            lines.add("registry lists only catalog-B within 15 s: " + gone);
            lines.add("shop sees 1 instance within 15 s: " + waitFor(() -> text(shop.port, "/instances").equals("1"), 15_000));
            lines.add("10 calls afterwards: " + calls(shop.port, 10));
            lines.add("-- an instance that crashes (SIGKILL)");
            Node catalogC = launch(nodes, CatalogApplication.class, "catalog", catalogArgs(eureka, "catalog-C"));
            waitFor(() -> text(shop.port, "/instances").equals("2"), 40_000);
            catalogC.process.destroyForcibly();
            catalogC.process.waitFor();
            long killed = System.currentTimeMillis();
            int failed = 0;
            int total = 0;
            long firstGood = -1;
            long lastFail = -1;
            while (System.currentTimeMillis() - killed < 15_000) {
                total++;
                if (status(shop.port, "/call") == 200) {
                    if (firstGood < 0) {
                        firstGood = System.currentTimeMillis();
                    }
                } else {
                    failed++;
                    lastFail = System.currentTimeMillis();
                }
                Thread.sleep(100);
            }
            lines.add("some calls failed while the registry still listed the dead instance: " + (failed > 0));
            lines.add("failed calls: " + failed + " of " + total + ", last failure " + (lastFail < 0 ? 0 : (lastFail - killed) / 1000) + " s after the crash");
            lines.add("failures stopped by themselves within 10 s: " + (lastFail < 0 || lastFail - killed < 10_000));
            lines.add("registry lists only catalog-B at the end: " + instanceIds(registry.port));
        } finally {
            for (Node node : nodes) {
                node.process.destroyForcibly();
            }
        }
        return lines;
    }

    private static String[] catalogArgs(String eureka, String id) {
        return new String[] {
            "--eureka.client.service-url.defaultZone=" + eureka,
            "--eureka.instance.instance-id=" + id,
            "--eureka.instance.lease-renewal-interval-in-seconds=1",
            "--eureka.instance.lease-expiration-duration-in-seconds=3",
            "--eureka.client.fetch-registry=false",
            "--eureka.client.jersey.enabled=false"
        };
    }

    private static Map<String, Integer> calls(int shopPort, int count) {
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < count; i++) {
            counts.merge(text(shopPort, "/call"), 1, Integer::sum);
        }
        return counts;
    }

    private static Node launch(List<Node> nodes, Class<?> main, String name, String... extra) throws Exception {
        int port = freePort();
        List<String> command = new ArrayList<>(List.of(
                System.getProperty("java.home") + "/bin/java",
                "-Xmx160m",
                "-classpath", System.getProperty("java.class.path"),
                main.getName(),
                "--server.port=" + port,
                "--spring.application.name=" + name,
                "--spring.main.banner-mode=off",
                "--logging.level.root=WARN"));
        if (name.equals("registry")) {
            command.add("--eureka.client.service-url.defaultZone=http://localhost:" + port + "/eureka/");
        }
        command.addAll(List.of(extra));
        File logs = new File("target/logs");
        logs.mkdirs();
        File log = new File(logs, name + "-" + port + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start();
        Node node = new Node(process, port);
        nodes.add(node);
        if (!waitFor(() -> status(port, "/") > 0, 60_000)) {
            throw new IllegalStateException(name + " did not start, see " + log);
        }
        return node;
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static boolean waitFor(BooleanSupplier condition, long millis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
            Thread.sleep(200);
        }
        return false;
    }

    private static String instanceIds(int registryPort) {
        String body = get(registryPort, "/eureka/apps/CATALOG", "application/json");
        List<String> ids = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"instanceId\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        Collections.sort(ids);
        return ids.toString();
    }

    private static String get(int port, String path, String accept) {
        try {
            HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Accept", accept).GET().build(), HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String text(int port, String path) {
        return get(port, path, "text/plain");
    }

    private static int status(int port, String path) {
        try {
            return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record Node(Process process, int port) {
    }
}
