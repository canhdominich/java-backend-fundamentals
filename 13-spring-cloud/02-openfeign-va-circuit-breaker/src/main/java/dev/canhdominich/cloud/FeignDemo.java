package dev.canhdominich.cloud;

import dev.canhdominich.cloud.order.OrderApplication;
import dev.canhdominich.cloud.order.PaymentClient;
import dev.canhdominich.cloud.payment.PaymentApplication;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class FeignDemo {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        ConfigurableApplicationContext payment = new SpringApplicationBuilder(PaymentApplication.class)
                .properties("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF")
                .run();
        String base = "http://localhost:" + ((WebServerApplicationContext) payment).getWebServer().getPort();
        try {
            lines.add("-- a declarative client");
            try (ConfigurableApplicationContext order = order(base)) {
                PaymentClient client = order.getBean(PaymentClient.class);
                lines.add("client bean is a generated proxy: " + client.getClass().getName().startsWith("jdk.proxy"));
                lines.add("pay(7) returned: " + client.pay(7));
                lines.add("hits on payment: " + hits(base));
                lines.add("-- errors become exceptions");
                lines.add("pay(404) threw: " + failure(client, 404));
                post(base, "/admin/mode/error");
                lines.add("payment down, pay(7) threw: " + failure(client, 7));
                post(base, "/admin/reset");
            }
            lines.add("-- timeouts");
            post(base, "/admin/mode/slow");
            try (ConfigurableApplicationContext order = order(base)) {
                lines.add("default timeouts, payment answers in 2 s: " + timed(order.getBean(PaymentClient.class), 7));
            }
            try (ConfigurableApplicationContext order = order(base, "spring.cloud.openfeign.client.config.payment.read-timeout=500")) {
                lines.add("read-timeout 500 ms: " + timed(order.getBean(PaymentClient.class), 7));
            }
            post(base, "/admin/reset");
            lines.add("-- the fallback is ignored while the circuit breaker is off");
            post(base, "/admin/mode/error");
            try (ConfigurableApplicationContext order = order(base)) {
                lines.add("fallback declared, breaker off, payment down: " + failure(order.getBean(PaymentClient.class), 7));
            }
            post(base, "/admin/reset");
            lines.add("-- circuit breaker on: the default time limit");
            try (ConfigurableApplicationContext order = order(base, breakerProperties())) {
                PaymentClient client = order.getBean(PaymentClient.class);
                lines.add("healthy payment: " + timed(client, 7));
                post(base, "/admin/mode/slow");
                lines.add("payment answers in 2 s: " + timed(client, 7));
                post(base, "/admin/reset");
            }
            lines.add("-- circuit breaker on: opening, failing fast, recovering");
            try (ConfigurableApplicationContext order = order(base, breakerProperties())) {
                PaymentClient client = order.getBean(PaymentClient.class);
                CircuitBreakerRegistry registry = order.getBean(CircuitBreakerRegistry.class);
                post(base, "/admin/mode/error");
                for (int i = 1; i <= 4; i++) {
                    lines.add("failing call " + i + ": " + timed(client, 7) + ", " + states(registry));
                }
                lines.add("payment hits so far: " + hits(base));
                lines.add("call 5 with the circuit open: " + timed(client, 7) + ", " + states(registry));
                lines.add("call 6 with the circuit open: " + timed(client, 7));
                lines.add("payment hits after calls 5 and 6: " + hits(base));
                post(base, "/admin/mode/ok");
                Thread.sleep(3500);
                lines.add("after 3.5 s with payment healthy: " + timed(client, 7) + ", " + states(registry));
                lines.add("next call: " + timed(client, 7) + ", " + states(registry));
            }
        } finally {
            payment.close();
        }
        return lines;
    }

    private static String[] breakerProperties() {
        return new String[] {
            "spring.cloud.openfeign.circuitbreaker.enabled=true",
            "resilience4j.circuitbreaker.configs.default.sliding-window-size=4",
            "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=4",
            "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
            "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=3s",
            "resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=2"
        };
    }

    private static ConfigurableApplicationContext order(String paymentUrl, String... extra) {
        List<String> properties = new ArrayList<>(List.of("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF", "payment.url=" + paymentUrl));
        properties.addAll(List.of(extra));
        return new SpringApplicationBuilder(OrderApplication.class).web(WebApplicationType.SERVLET).properties(properties.toArray(String[]::new)).run();
    }

    private static String failure(PaymentClient client, long id) {
        try {
            return client.pay(id);
        } catch (FeignException e) {
            return e.getClass().getSimpleName() + ", status " + e.status();
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + " caused by " + (e.getCause() == null ? "nothing" : e.getCause().getClass().getSimpleName());
        }
    }

    private static String timed(PaymentClient client, long id) {
        long start = System.nanoTime();
        String result = failure(client, id);
        long ms = (System.nanoTime() - start) / 1_000_000;
        return result + " in " + (ms < 50 ? "under 50" : String.valueOf(Math.round(ms / 250.0) * 250)) + " ms";
    }

    private static String states(CircuitBreakerRegistry registry) {
        List<String> states = new ArrayList<>();
        for (CircuitBreaker breaker : registry.getAllCircuitBreakers()) {
            states.add(breaker.getName() + "=" + breaker.getState().name());
        }
        return "circuit " + states;
    }

    private static String hits(String base) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(base + "/admin/hits")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static void post(String base, String path) throws Exception {
        HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
    }
}
