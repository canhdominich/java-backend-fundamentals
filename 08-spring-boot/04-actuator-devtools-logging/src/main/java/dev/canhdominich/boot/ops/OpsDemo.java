package dev.canhdominich.boot.ops;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class OpsDemo {

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("-- default exposure");
        try (Server server = Server.start()) {
            lines.add("GET /actuator -> " + server.get("/actuator").summary(body -> "links " + new TreeSet<>(find(body, "\"([a-z]+)\":\\{\"href\"", 1))));
            lines.add("GET /actuator/health -> " + server.get("/actuator/health").summary(body -> body));
            lines.add("GET /actuator/metrics -> " + server.get("/actuator/metrics").status());
            lines.add("GET /actuator/env -> " + server.get("/actuator/env").status());
        }
        lines.add("-- exposed: health with details, metrics, env, loggers");
        try (Server server = Server.start("--management.endpoints.web.exposure.include=health,info,metrics,env,loggers", "--management.endpoint.health.show-details=always")) {
            server.post("/orders", "");
            server.post("/orders", "");
            server.post("/orders", "");
            Reply health = server.get("/actuator/health");
            lines.add("GET /actuator/health -> " + health.status() + " components " + new TreeSet<>(find(health.body(), "\"([a-zA-Z]+)\":\\{\"(?:details|status)\"", 1)));
            lines.add("queue component: " + first(health.body(), "\"queue\":\\{\"details\":\\{[^}]*\\},\"status\":\"[A-Z]+\"\\}"));
            Reply metric = server.get("/actuator/metrics/orders.created");
            lines.add("GET /actuator/metrics/orders.created after 3 orders -> " + first(metric.body(), "\"value\":[0-9.]+"));
            lines.add("jvm.memory.used is a known meter: " + (server.get("/actuator/metrics").body().contains("jvm.memory.used")));
            lines.add("env app.name -> " + first(server.get("/actuator/env/app.name").body(), "\"property\":\\{[^}]*\\}"));
            lines.add("env app.password -> " + first(server.get("/actuator/env/app.password").body(), "\"property\":\\{[^}]*\\}"));
            server.post("/queue?depth=5000", "");
            Reply down = server.get("/actuator/health");
            lines.add("queue depth 5000 -> GET /actuator/health " + down.status() + " " + first(down.body(), "\"queue\":\\{\"details\":\\{[^}]*\\},\"status\":\"[A-Z]+\"\\}"));
            server.post("/queue?depth=0", "");
            lines.add("-- env values with show-values=ALWAYS");
        try (Server visible = Server.start("--management.endpoints.web.exposure.include=env", "--management.endpoint.env.show-values=ALWAYS")) {
            lines.add("env app.name -> " + first(visible.get("/actuator/env/app.name").body(), "\"value\":\"[^\"]*\""));
            lines.add("env app.password -> " + first(visible.get("/actuator/env/app.password").body(), "\"value\":\"[^\"]*\""));
        }
        lines.add("-- changing a log level while the application runs");
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            Logger logger = (Logger) LoggerFactory.getLogger(OrderController.class);
            logger.addAppender(appender);
            server.post("/orders", "");
            lines.add("default level: " + messages(appender));
            appender.list.clear();
            lines.add("POST /actuator/loggers/" + OrderController.class.getName() + " DEBUG -> " + server.postJson("/actuator/loggers/" + OrderController.class.getName(), "{\"configuredLevel\":\"DEBUG\"}").status());
            server.post("/orders", "");
            lines.add("after DEBUG: " + messages(appender));
            logger.detachAppender(appender);
            logger.setLevel(null);
        }
        lines.add("-- structured logging in a separate process");
        lines.add(structured());
        lines.add("-- devtools");
        try (Server server = Server.start()) {
            lines.add("class loader of the application classes -> " + server.get("/loader").body());
        }
        return lines;
    }

    private static List<String> messages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(event -> event.getLevel() + " " + event.getFormattedMessage()).toList();
    }

    private static List<String> find(String text, String regex, int group) {
        List<String> found = new ArrayList<>();
        Matcher matcher = Pattern.compile(regex).matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(group));
        }
        return found;
    }

    private static String first(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group() : "(no match)";
    }

    private static String structured() throws Exception {
        List<String> command = new ArrayList<>(List.of(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java", "-cp", System.getProperty("java.class.path"),
                "-Dspring.devtools.restart.enabled=false", StructuredMain.class.getName(), "--logging.structured.format.console=logstash"));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        String json = output.lines().filter(line -> line.startsWith("{") && line.contains("order accepted")).findFirst().orElse("(no structured line)");
        String keys = String.join(", ", new TreeSet<>(find(json, "\"([@a-zA-Z_.]+)\":", 1)));
        return "logstash format, keys of one line: " + keys;
    }

    record Reply(int status, String body) {

        String summary(java.util.function.Function<String, String> formatter) {
            return status + " " + formatter.apply(body);
        }
    }

    static final class Server implements AutoCloseable {

        private final ConfigurableApplicationContext context;
        private final int port;
        private final HttpClient http = HttpClient.newHttpClient();

        private Server(ConfigurableApplicationContext context) {
            this.context = context;
            this.port = ((WebServerApplicationContext) context).getWebServer().getPort();
        }

        static Server start(String... args) {
            SpringApplication application = new SpringApplication(OpsApplication.class);
            application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "WARN", "logging.level.dev.canhdominich", "INFO"));
            return new Server(application.run(args));
        }

        Reply get(String path) throws Exception {
            return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build());
        }

        Reply post(String path, String body) throws Exception {
            return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).POST(HttpRequest.BodyPublishers.ofString(body)).build());
        }

        Reply postJson(String path, String json) throws Exception {
            return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build());
        }

        private Reply send(HttpRequest request) throws Exception {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        }

        @Override
        public void close() {
            context.close();
        }
    }

    public static final class StructuredMain {

        public static void main(String[] args) {
            SpringApplication application = new SpringApplication(OpsApplication.class);
            application.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.dev.canhdominich", "INFO"));
            try (ConfigurableApplicationContext context = application.run(args)) {
                context.getBean(OrderController.class).create();
            }
        }
    }
}
