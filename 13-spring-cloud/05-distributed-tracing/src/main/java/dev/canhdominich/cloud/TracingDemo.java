package dev.canhdominich.cloud;

import brave.handler.MutableSpan;
import dev.canhdominich.cloud.front.FrontApplication;
import dev.canhdominich.cloud.inventory.InventoryApplication;
import dev.canhdominich.cloud.pricing.PricingApplication;
import dev.canhdominich.cloud.trace.SpanCollector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class TracingDemo {

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
        lines.add("-- default sampling");
        try (Chain chain = new Chain(null)) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                seen.add(field(get(chain.front, "/checkout", null), "front", "traceId"));
            }
            Thread.sleep(500);
            Set<String> exported = SpanCollector.FINISHED.stream().map(MutableSpan::traceId).collect(Collectors.toSet());
            lines.add("100 requests carried " + seen.size() + " different trace ids, none missing: " + !seen.contains("none"));
            lines.add("traces actually recorded: " + exported.size() + " of 100");
        }
        SpanCollector.FINISHED.clear();
        lines.add("-- every request recorded");
        try (Chain chain = new Chain("1.0")) {
            String body = get(chain.front, "/checkout", null);
            String trace = field(body, "front", "traceId");
            lines.add("services in the answer: " + services(body));
            lines.add("same trace id in all three: " + (trace.equals(field(body, "inventory", "traceId")) && trace.equals(field(body, "pricing", "traceId"))));
            lines.add("span ids all different: " + (new HashSet<>(List.of(field(body, "front", "spanId"), field(body, "inventory", "spanId"), field(body, "pricing", "spanId"))).size() == 3));
            lines.add("front parent: " + field(body, "front", "parentId"));
            lines.add("header received by inventory carries the same trace id: " + field(body, "inventory", "incoming").contains(trace));
            lines.add("trace id written to the log context (MDC) equals the tracer's: " + trace.equals(field(body, "pricing", "mdcTraceId")));
            Thread.sleep(800);
            List<MutableSpan> spans = SpanCollector.FINISHED.stream().filter(s -> trace.equals(s.traceId())).toList();
            lines.add("spans recorded for the request: " + spans.stream().map(s -> s.localServiceName() + ":" + s.kind()).sorted().toList());
            Set<String> ids = spans.stream().map(MutableSpan::id).collect(Collectors.toSet());
            long orphans = spans.stream().filter(s -> s.parentId() != null && !ids.contains(s.parentId())).count();
            lines.add("spans whose parent is missing from the trace: " + orphans);
            lines.add("second request has a different trace id: " + !trace.equals(field(get(chain.front, "/checkout", null), "front", "traceId")));
            String incoming = "4bf92f3577b34da6a3ce929d0e0e4736";
            String outside = get(chain.front, "/checkout", "00-" + incoming + "-00f067aa0ba902b7-01");
            lines.add("a trace started outside is continued: " + (incoming.equals(field(outside, "front", "traceId")) && incoming.equals(field(outside, "pricing", "traceId"))));
            lines.add("front's parent is the outside span: " + field(outside, "front", "parentId").equals("00f067aa0ba902b7"));
            lines.add("-- crossing a thread");
            lines.add(get(chain.pricing, "/async", null).replace(" other", ", other"));
        }
        SpanCollector.FINISHED.clear();
        lines.add("-- sampling switched off");
        try (Chain chain = new Chain("0.0")) {
            String body = get(chain.front, "/checkout", null);
            Thread.sleep(500);
            lines.add("trace id still present: " + !field(body, "front", "traceId").equals("none"));
            lines.add("spans recorded: " + SpanCollector.FINISHED.size());
        }
        return lines;
    }

    private static String services(String body) {
        List<String> names = new ArrayList<>();
        Matcher matcher = Pattern.compile("service=(\\w+)").matcher(body);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names.toString();
    }

    private static String field(String body, String service, String name) {
        for (String line : body.split("\n")) {
            if (line.startsWith("service=" + service + " ")) {
                Matcher matcher = Pattern.compile(" " + name + "=(\\S+)").matcher(line);
                return matcher.find() ? matcher.group(1) : "absent";
            }
        }
        return "no such service";
    }

    private static String get(int port, String path, String traceparent) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (traceparent != null) {
            request.header("traceparent", traceparent);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static final class Chain implements AutoCloseable {

        final ConfigurableApplicationContext pricingContext;
        final ConfigurableApplicationContext inventoryContext;
        final ConfigurableApplicationContext frontContext;
        final int pricing;
        final int inventory;
        final int front;

        Chain(String probability) {
            pricingContext = start(PricingApplication.class, "pricing", probability);
            pricing = port(pricingContext);
            inventoryContext = start(InventoryApplication.class, "inventory", probability, "pricing.url=http://localhost:" + pricing);
            inventory = port(inventoryContext);
            frontContext = start(FrontApplication.class, "front", probability, "inventory.url=http://localhost:" + inventory);
            front = port(frontContext);
        }

        private static ConfigurableApplicationContext start(Class<?> application, String name, String probability, String... extra) {
            List<String> properties = new ArrayList<>(List.of("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF", "spring.application.name=" + name));
            if (probability != null) {
                properties.add("management.tracing.sampling.probability=" + probability);
            }
            properties.addAll(List.of(extra));
            return new SpringApplicationBuilder(application).properties(properties.toArray(String[]::new)).run();
        }

        private static int port(ConfigurableApplicationContext context) {
            return ((WebServerApplicationContext) context).getWebServer().getPort();
        }

        @Override
        public void close() {
            frontContext.close();
            inventoryContext.close();
            pricingContext.close();
        }
    }
}
