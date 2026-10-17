package dev.canhdominich.micro;

import dev.canhdominich.micro.inventory.InventoryApplication;
import dev.canhdominich.micro.monolith.MonolithApplication;
import dev.canhdominich.micro.order.OrderApplication;
import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComparisonDemo {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
        System.exit(0);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        try {
            long t0 = System.nanoTime();
            Node monolith = launch(nodes, MonolithApplication.class, "/stock/BOOK");
            double monolithStart = (System.nanoTime() - t0) / 1e9;
            long t1 = System.nanoTime();
            Node inventory = launch(nodes, InventoryApplication.class, "/stock/BOOK");
            Node order = launch(nodes, OrderApplication.class, "/orders/count", "--inventory.url=http://localhost:" + inventory.port);
            double microStart = (System.nanoTime() - t1) / 1e9;
            for (int i = 0; i < 30; i++) {
                post(monolith.port, "/orders?sku=BOOK&quantity=1");
                post(order.port, "/orders?sku=BOOK&quantity=1");
            }
            lines.add("-- cost of running it");
            lines.add("processes: monolith 1, microservices 2");
            lines.add("memory after warm-up: monolith " + round10(rssMb(monolith)) + " MB, microservices " + round10(rssMb(inventory) + rssMb(order)) + " MB");
            lines.add("time until ready: monolith " + roundHalf(monolithStart) + " s, microservices (started one after the other) " + roundHalf(microStart) + " s");
            lines.add("-- cost of every call");
            long[] mono = timings(monolith.port, 300);
            long[] micro = timings(order.port, 300);
            lines.add("median of 300 orders: monolith " + mono[0] + " ms, microservices " + micro[0] + " ms");
            lines.add("one extra network hop makes the median slower: " + (micro[0] > mono[0]));
            lines.add("-- an order that fails after the stock was reserved");
            int monoStockBefore = Integer.parseInt(get(monolith.port, "/stock/BOOK"));
            int monoOrdersBefore = Integer.parseInt(get(monolith.port, "/orders/count"));
            int monoStatus = post(monolith.port, "/orders?sku=BOOK&quantity=5&failAfterReserve=true");
            lines.add("monolith: status " + monoStatus + ", stock change " + (Integer.parseInt(get(monolith.port, "/stock/BOOK")) - monoStockBefore)
                    + ", orders change " + (Integer.parseInt(get(monolith.port, "/orders/count")) - monoOrdersBefore));
            int microStockBefore = Integer.parseInt(get(inventory.port, "/stock/BOOK"));
            int microOrdersBefore = Integer.parseInt(get(order.port, "/orders/count"));
            int microStatus = post(order.port, "/orders?sku=BOOK&quantity=5&failAfterReserve=true");
            lines.add("microservices: status " + microStatus + ", stock change " + (Integer.parseInt(get(inventory.port, "/stock/BOOK")) - microStockBefore)
                    + ", orders change " + (Integer.parseInt(get(order.port, "/orders/count")) - microOrdersBefore));
            lines.add("-- inventory service stops");
            inventory.process.destroyForcibly();
            inventory.process.waitFor();
            lines.add("order service, placing an order: status " + post(order.port, "/orders?sku=BOOK&quantity=1"));
            lines.add("order service, reading its own data: status " + status(order.port, "/orders/count"));
        } finally {
            for (Node node : nodes) {
                node.process.destroyForcibly();
            }
        }
        return lines;
    }

    private static long[] timings(int port, int count) throws Exception {
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            post(port, "/orders?sku=BOOK&quantity=1");
            values[i] = (System.nanoTime() - start) / 1000;
        }
        Arrays.sort(values);
        return new long[] {Math.round(values[count / 2] / 1000.0), Math.round(values[(int) (count * 0.95)] / 1000.0)};
    }

    private static long rssMb(Node node) throws Exception {
        for (String line : Files.readAllLines(Path.of("/proc/" + node.process.pid() + "/status"))) {
            if (line.startsWith("VmRSS:")) {
                return Long.parseLong(line.replaceAll("\\D+", "")) / 1024;
            }
        }
        return -1;
    }

    private static long round10(long value) {
        return Math.round(value / 10.0) * 10;
    }

    private static String roundHalf(double seconds) {
        return String.valueOf(Math.round(seconds * 2) / 2.0);
    }

    private static Node launch(List<Node> nodes, Class<?> main, String readyPath, String... extra) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        List<String> command = new ArrayList<>(List.of(
                System.getProperty("java.home") + "/bin/java", "-Xmx128m", "-XX:+UseSerialGC",
                "-classpath", System.getProperty("java.class.path"), main.getName(),
                "--server.port=" + port, "--spring.main.banner-mode=off", "--logging.level.root=WARN"));
        command.addAll(List.of(extra));
        File logs = new File("target/logs");
        logs.mkdirs();
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(new File(logs, main.getSimpleName() + ".log")).start();
        Node node = new Node(process, port);
        nodes.add(node);
        long deadline = System.currentTimeMillis() + 90_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (status(port, readyPath) == 200) {
                    return node;
                }
            } catch (Exception ignored) {
                Thread.sleep(100);
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException(main.getSimpleName() + " did not start, see target/logs");
    }

    private static String get(int port, String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static int status(int port, String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static int post(int port, String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    record Node(Process process, int port) {
    }
}
