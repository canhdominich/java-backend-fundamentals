package dev.canhdominich.jsp.intro;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.apache.catalina.Context;

public final class JspProbes {

    static final int INCREMENTS = 2000;

    private JspProbes() {
    }

    public static void main(String[] args) throws Exception {
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            HttpClient client = HttpClient.newHttpClient();
            String base = "http://127.0.0.1:" + server.port();

            System.out.println("-- hello.jsp, first request compiles the page");
            long first = timed(client, base + "/hello.jsp");
            long second = timed(client, base + "/hello.jsp");
            System.out.println("first request " + first + " ms, second request " + second + " ms, first slower by at least 5x: " + (first > 5 * second));
            System.out.print(get(client, base + "/hello.jsp").body());

            System.out.println("-- the servlet that Jasper generated for hello.jsp");
            for (String line : generatedLines(server, "hello_jsp.java")) {
                System.out.println("  " + line);
            }

            System.out.println("-- a declaration is a field of the generated servlet: " + INCREMENTS + " increments from 16 threads");
            System.out.println("  final value " + hammer(client, base));

            System.out.println("-- does the page create a session?");
            System.out.println("  default page    Set-Cookie: " + get(client, base + "/session-default.jsp").headers().firstValue("Set-Cookie").orElse("none"));
            System.out.println("  session=\"false\" Set-Cookie: " + get(client, base + "/session-off.jsp").headers().firstValue("Set-Cookie").orElse("none"));

            System.out.println("-- printing the same request parameter three ways");
            System.out.print(get(client, base + "/xss.jsp?q=%3Cscript%3Ealert(1)%3C/script%3E").body());

            System.out.println("-- editing a page while the server runs");
            dynamicReload(server, client, base);

            System.out.println("-- a page that does not compile");
            HttpResponse<String> broken = get(client, base + "/broken.jsp");
            System.out.println("  status " + broken.statusCode() + ", body mentions the source line with secretPrice: " + broken.body().contains("secretPrice"));
        }
    }

    static List<String> generatedLines(EmbeddedServer server, String fileName) throws IOException {
        try (Stream<Path> files = Files.walk(server.workDirectory().toPath())) {
            Path generated = files.filter(path -> path.getFileName().toString().equals(fileName)).findFirst().orElseThrow();
            List<String> keep = new ArrayList<>();
            for (String line : Files.readAllLines(generated)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("public final class") || trimmed.startsWith("public void _jspService") || trimmed.startsWith("out.write(") || trimmed.startsWith("out.print(") || trimmed.startsWith("for (int i")) {
                    keep.add(trimmed.length() > 110 ? trimmed.substring(0, 110) + "..." : trimmed);
                }
            }
            return keep;
        }
    }

    static String hammer(HttpClient client, String base) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < INCREMENTS; i++) {
            futures.add(pool.submit(() -> get(client, base + "/counter.jsp")));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();
        return get(client, base + "/counter.jsp?peek=1").body().trim();
    }

    static void dynamicReload(EmbeddedServer server, HttpClient client, String base) throws Exception {
        Path directory = Files.createTempDirectory("jsp-dynamic");
        Path page = directory.resolve("dynamic.jsp");
        Files.writeString(page, "<%@ page contentType=\"text/plain;charset=UTF-8\" session=\"false\" %>version 1\n", StandardCharsets.UTF_8);
        Context context = server.tomcat().addWebapp("/dyn", directory.toString());
        System.out.println("  before the edit     : " + get(client, base + "/dyn/dynamic.jsp").body().trim());
        Files.writeString(page, "<%@ page contentType=\"text/plain;charset=UTF-8\" session=\"false\" %>version 2\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(page, FileTime.fromMillis(System.currentTimeMillis() + 5000));
        System.out.println("  right after the edit: " + get(client, base + "/dyn/dynamic.jsp").body().trim());
        Thread.sleep(5000);
        System.out.println("  5 seconds later     : " + get(client, base + "/dyn/dynamic.jsp").body().trim());
        context.getParent().removeChild(context);
    }

    private static long timed(HttpClient client, String url) throws Exception {
        long start = System.nanoTime();
        get(client, url);
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
