package dev.canhdominich.spring.mvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class DispatcherDemo {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        try (EmbeddedServer server = EmbeddedServer.start(0, WebConfig.class)) {
            AnnotationConfigWebApplicationContext context = server.springContext();
            lines.add("handler mappings: " + new TreeSet<>(context.getBeansOfType(HandlerMapping.class).keySet()));
            lines.add("handler adapters: " + new TreeSet<>(context.getBeansOfType(HandlerAdapter.class).keySet()));
            lines.add("view resolvers: " + new TreeSet<>(context.getBeansOfType(ViewResolver.class).keySet()));
            RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);
            mapping.getHandlerMethods().forEach((info, method) -> lines.add("mapped: " + info + " -> " + method.getMethod().getName()));
            lines.forEach(System.out::println);
            lines.add(call(server, "GET", "/hello"));
            lines.add(call(server, "GET", "/nope"));
            lines.add(call(server, "POST", "/hello"));
            lines.add(call(server, "GET", "/boom"));
            lines.add(call(server, "GET", "/WEB-INF/views/hello.jsp"));
        }
        return lines;
    }

    private static String call(EmbeddedServer server, String method, String path) throws Exception {
        Trace.clear();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        StringBuilder out = new StringBuilder(method + " " + path + " -> " + response.statusCode());
        response.headers().firstValue("Allow").ifPresent(allow -> out.append(", Allow: ").append(allow));
        if (method.equals("GET") && path.equals("/hello")) {
            out.append(", body: ").append(response.body().replaceAll("\\s+", " ").trim());
        }
        for (String event : Trace.all()) {
            out.append("\n    ").append(event);
        }
        System.out.println(out);
        return out.toString();
    }
}
