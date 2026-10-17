package dev.canhdominich.boot.tour;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.web.servlet.HandlerMapping;

public class TourDemo {

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        long start = System.nanoTime();
        SpringApplication application = new SpringApplication(Application.class);
        application.setDefaultProperties(java.util.Map.of("server.port", "0", "spring.main.banner-mode", "off", "logging.level.root", "WARN"));
        try (ConfigurableApplicationContext context = application.run()) {
            long millis = (System.nanoTime() - start) / 1_000_000;
            lines.add("spring boot " + SpringBootVersion.getVersion() + ", spring framework " + SpringVersion.getVersion() + ", tomcat " + org.apache.catalina.util.ServerInfo.getServerNumber());
            String[] beanNames = context.getBeanDefinitionNames();
            Set<String> yours = Arrays.stream(beanNames)
                    .filter(name -> context.getBean(name).getClass().getName().startsWith("dev.canhdominich"))
                    .collect(Collectors.toCollection(TreeSet::new));
            lines.add("beans in the context: " + (beanNames.length > 100 ? "more than 100" : beanNames.length) + ", declared by this project's classes: " + yours);
            ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
            List<String> matched = report.getConditionAndOutcomesBySource().entrySet().stream()
                    .filter(entry -> entry.getValue().isFullMatch())
                    .map(entry -> entry.getKey())
                    .filter(name -> name.endsWith("AutoConfiguration"))
                    .map(name -> name.substring(name.lastIndexOf('.') + 1))
                    .sorted()
                    .toList();
            lines.add("auto-configurations that applied: " + matched.size() + " " + matched);
            for (String name : List.of("DispatcherServletAutoConfiguration", "TomcatServletWebServerAutoConfiguration", "WebMvcAutoConfiguration", "JacksonAutoConfiguration", "HttpMessageConvertersAutoConfiguration", "ErrorMvcAutoConfiguration")) {
                lines.add("  " + name + ": " + (matched.contains(name) ? "applied" : "not applied"));
            }
            List<String> skipped = report.getConditionAndOutcomesBySource().entrySet().stream()
                    .filter(entry -> !entry.getValue().isFullMatch())
                    .filter(entry -> entry.getKey().endsWith("AutoConfiguration"))
                    .map(entry -> entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1))
                    .sorted()
                    .toList();
            lines.add("auto-configurations whose conditions did not match: " + skipped.size() + " " + skipped);
            lines.add("handler mappings: " + new TreeSet<>(context.getBeansOfType(HandlerMapping.class).keySet()));
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            lines.add("embedded server listening on a port chosen by the OS: " + (port > 0));
            lines.add(call(port, "/hello"));
            lines.add(call(port, "/nope"));
            lines.add("startup under 20 seconds: " + (millis < 20_000));
        }
        return lines;
    }

    private static String call(int port, String path) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        String body = response.body().replaceAll("\"timestamp\":\"[^\"]*\",?", "");
        return "GET " + path + " -> " + response.statusCode() + " " + response.headers().firstValue("Content-Type").orElse("") + " " + body;
    }
}
