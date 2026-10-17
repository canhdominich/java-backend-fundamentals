package dev.canhdominich.cloud;

import dev.canhdominich.cloud.catalog.CatalogApplication;
import dev.canhdominich.cloud.gateway.GatewayApplication;
import dev.canhdominich.cloud.order.OrderApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class GatewayDemo {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String KEY = "edge-secret";

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        ConfigurableApplicationContext catalog1 = backend(CatalogApplication.class, "instance.name=catalog-1");
        ConfigurableApplicationContext catalog2 = backend(CatalogApplication.class, "instance.name=catalog-2");
        ConfigurableApplicationContext order = backend(OrderApplication.class);
        ConfigurableApplicationContext gateway = new SpringApplicationBuilder(GatewayApplication.class)
                .properties("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF",
                        "catalog.url=http://localhost:" + port(catalog1),
                        "catalog2.url=http://localhost:" + port(catalog2),
                        "order.url=http://localhost:" + port(order))
                .run();
        int gw = port(gateway);
        try {
            lines.add("-- routing by path and removing a prefix");
            lines.add("GET /api/catalog/products: " + show(get(gw, "/api/catalog/products", KEY)));
            lines.add("what the backend received: " + get(gw, "/api/catalog/echo", KEY).body());
            lines.add("response header added by the gateway: X-Served-By=" + get(gw, "/api/catalog/products", KEY).headers().firstValue("X-Served-By").orElse("none"));
            lines.add("-- rewriting a path");
            lines.add("GET /api/orders/5: " + show(get(gw, "/api/orders/5", KEY)));
            lines.add("-- a path with no route");
            lines.add("GET /nothing: status " + get(gw, "/nothing", null).statusCode());
            lines.add("-- a route that balances over two instances");
            Map<String, Integer> counts = new TreeMap<>();
            for (int i = 0; i < 6; i++) {
                counts.merge(get(gw, "/lb/catalog/products", null).body(), 1, Integer::sum);
            }
            lines.add("6 calls to /lb/catalog/products: " + counts);
            lines.add("-- a filter at the edge");
            int before = Integer.parseInt(direct(catalog1, "/admin/hits"));
            lines.add("GET /api/catalog/products without key: status " + get(gw, "/api/catalog/products", null).statusCode());
            lines.add("GET /api/catalog/products with wrong key: status " + get(gw, "/api/catalog/products", "guess").statusCode());
            lines.add("backend hits changed by the rejected calls: " + (Integer.parseInt(direct(catalog1, "/admin/hits")) - before));
            lines.add("GET /api/catalog/products with key: status " + get(gw, "/api/catalog/products", KEY).statusCode());
            lines.add("calling a backend directly, bypassing the gateway: status " + get(port(catalog1), "/products", null).statusCode());
            lines.add("-- a backend that is down");
            order.close();
            lines.add("GET /api/orders/5 after stopping order: status " + get(gw, "/api/orders/5", KEY).statusCode());
        } finally {
            gateway.close();
            catalog1.close();
            catalog2.close();
            order.close();
        }
        return lines;
    }

    private static ConfigurableApplicationContext backend(Class<?> application, String... extra) {
        List<String> properties = new ArrayList<>(List.of("server.port=0", "spring.config.name=backend", "spring.main.banner-mode=off", "logging.level.root=OFF"));
        properties.addAll(List.of(extra));
        return new SpringApplicationBuilder(application).properties(properties.toArray(String[]::new)).run();
    }

    private static int port(ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private static String direct(ConfigurableApplicationContext context, String path) throws Exception {
        return get(port(context), path, null).body();
    }

    private static HttpResponse<String> get(int port, String path, String key) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (key != null) {
            request.header("X-Api-Key", key);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String show(HttpResponse<String> response) {
        return response.statusCode() + " " + response.body();
    }
}
