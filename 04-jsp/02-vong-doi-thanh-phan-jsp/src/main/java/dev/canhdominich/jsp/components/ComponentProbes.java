package dev.canhdominich.jsp.components;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class ComponentProbes {

    private ComponentProbes() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("-- lifecycle of a JSP: two requests, then the server stops");
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            HttpClient client = HttpClient.newHttpClient();
            String base = "http://127.0.0.1:" + server.port();
            get(client, base + "/lifecycle.jsp");
            get(client, base + "/lifecycle.jsp");

            System.out.println("-- static include and dynamic include");
            System.out.print(get(client, base + "/includes.jsp").body());
            String source = generatedSource(server, "includes_jsp.java");
            System.out.println("generated includes_jsp.java contains the static header text itself : " + source.contains("static header text"));
            System.out.println("generated includes_jsp.java calls the dynamic header at request time: " + source.contains("JspRuntimeLibrary.include(request, response, _jspx_temp0_url"));

            System.out.println("-- which scope does ${who} read? (page|requestScope|sessionScope|applicationScope shown after it)");
            for (String[] combo : new String[][]{{"", ""}, {"application", ""}, {"session,application", ""}, {"request,session,application", ""}, {"request,session,application", "yes"}}) {
                HttpClient fresh = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
                String url = base + "/scopes?set=" + combo[0] + (combo[1].isEmpty() ? "" : "&page=" + combo[1]);
                System.out.printf("  set=%-30s page=%-4s -> %s%n", combo[0].isEmpty() ? "(nothing)" : combo[0], combo[1].isEmpty() ? "no" : combo[1], get(fresh, url).body().trim());
            }

            HttpClient plain = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
            System.out.println("-- the same attributes read by a page with session=\"false\"");
            get(plain, base + "/scopes?set=session");
            System.out.println("  page with a session : " + get(plain, base + "/scopes.jsp").body().trim());
            System.out.println("  page session=false  : " + get(plain, base + "/scopes-nosession.jsp").body().trim());

            System.out.println("-- JSTL and EL on a list of records");
            System.out.print(get(client, base + "/products").body());

            System.out.println("-- error page");
            HttpResponse<String> failed = get(client, base + "/throws.jsp");
            System.out.println("status " + failed.statusCode());
            System.out.print(failed.body());

            System.out.println("-- jsp:useBean in the session scope");
            HttpClient session = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
            System.out.print(get(session, base + "/bean.jsp").body());
            System.out.print(get(session, base + "/bean.jsp").body());
            System.out.println("-- stopping the server");
        }
    }

    static String generatedSource(EmbeddedServer server, String fileName) throws IOException {
        try (Stream<Path> files = Files.walk(server.workDirectory().toPath())) {
            return Files.readString(files.filter(path -> path.getFileName().toString().equals(fileName)).findFirst().orElseThrow());
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
