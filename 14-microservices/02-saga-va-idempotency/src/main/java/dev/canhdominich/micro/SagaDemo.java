package dev.canhdominich.micro;

import dev.canhdominich.micro.inventory.InventoryApplication;
import dev.canhdominich.micro.order.OrderApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class SagaDemo {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        try {
            run().forEach(System.out::println);
        } catch (Throwable e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            System.out.println("failed: " + root);
        }
        System.exit(0);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("-- failure after the stock was reserved");
        try (Rig rig = new Rig()) {
            int status = rig.order("o1", false, true);
            lines.add("without compensation: " + rig.summary(status));
        }
        try (Rig rig = new Rig()) {
            int status = rig.order("o1", true, true);
            lines.add("with compensation: " + rig.summary(status));
        }
        lines.add("-- the first response is slow, the caller times out and retries");
        try (Rig rig = new Rig()) {
            rig.post(rig.inventory, "/admin/delay?millis=1500&calls=1");
            int status = rig.order("o2", false, false);
            lines.add("retry without a key: " + rig.summary(status));
        }
        try (Rig rig = new Rig()) {
            rig.post(rig.inventory, "/admin/delay?millis=1500&calls=1");
            int status = rig.order("o2", true, false);
            lines.add("retry with a key: " + rig.summary(status));
        }
        lines.add("-- every response is slow, so every attempt times out");
        try (Rig rig = new Rig()) {
            rig.post(rig.inventory, "/admin/delay?millis=1500&calls=10");
            int status = rig.order("o3", true, false);
            lines.add("keyed reserve, unknown outcome, compensated: " + rig.summary(status));
        }
        lines.add("-- the compensation itself cannot be delivered");
        try (Rig rig = new Rig()) {
            rig.post(rig.inventory, "/admin/release-down?value=true");
            int status = rig.order("o4", true, true);
            lines.add("release is down: " + rig.summary(status) + ", pending compensations " + rig.get(rig.order, "/compensations/pending"));
            rig.post(rig.inventory, "/admin/release-down?value=false");
            lines.add("compensations delivered by the retry: " + rig.post(rig.order, "/compensations/retry"));
            lines.add("after the retry: stock change " + rig.stockChange() + ", pending compensations " + rig.get(rig.order, "/compensations/pending"));
        }
        lines.add("-- the compensation is repeated");
        try (Rig rig = new Rig()) {
            rig.order("o5", true, true);
            lines.add("the order service already released it; releasing by hand: " + rig.post(rig.inventory, "/release?key=o5") + ", stock change " + rig.stockChange());
            lines.add("releasing a key that never reserved: " + rig.post(rig.inventory, "/release?key=unknown"));
        }
        return lines;
    }

    private static final class Rig implements AutoCloseable {

        final ConfigurableApplicationContext inventoryContext;
        final ConfigurableApplicationContext orderContext;
        final int inventory;
        final int order;

        Rig() {
            inventoryContext = start(InventoryApplication.class);
            inventory = port(inventoryContext);
            orderContext = start(OrderApplication.class, "inventory.url=http://localhost:" + inventory, "spring.http.clients.read-timeout=500ms");
            order = port(orderContext);
        }

        int order(String id, boolean safe, boolean fail) throws Exception {
            return status("POST", order, "/orders?orderId=" + id + "&sku=BOOK&quantity=5&safe=" + safe + "&failAfterReserve=" + fail);
        }

        String summary(int status) throws Exception {
            return "status " + status + ", stock change " + stockChange() + ", orders " + get(order, "/orders/count");
        }

        int stockChange() throws Exception {
            return Integer.parseInt(get(inventory, "/stock/BOOK")) - 100;
        }

        String get(int port, String path) throws Exception {
            return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        }

        String post(int port, String path) throws Exception {
            return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).body();
        }

        int status(String method, int port, String path) throws Exception {
            return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        }

        private static ConfigurableApplicationContext start(Class<?> application, String... extra) {
            List<String> properties = new ArrayList<>(List.of("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF"));
            properties.addAll(List.of(extra));
            return new SpringApplicationBuilder(application).properties(properties.toArray(String[]::new)).run();
        }

        private static int port(ConfigurableApplicationContext context) {
            return ((WebServerApplicationContext) context).getWebServer().getPort();
        }

        @Override
        public void close() {
            orderContext.close();
            inventoryContext.close();
        }
    }
}
