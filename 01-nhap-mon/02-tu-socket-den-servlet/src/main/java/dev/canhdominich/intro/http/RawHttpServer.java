package dev.canhdominich.intro.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RawHttpServer implements AutoCloseable {

    private final ServerSocket server;
    private volatile boolean running = true;

    public RawHttpServer() throws IOException {
        server = new ServerSocket(0);
        Thread thread = new Thread(this::loop, "raw-http-server");
        thread.setDaemon(true);
        thread.start();
    }

    public int port() {
        return server.getLocalPort();
    }

    private void loop() {
        while (running) {
            try (Socket socket = server.accept()) {
                handle(socket);
            } catch (IOException e) {
                if (!running) {
                    return;
                }
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        String requestLine = in.readLine();
        if (requestLine == null) {
            return;
        }
        String[] parts = requestLine.split(" ");
        String target = parts[1];
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            headers.put(line.substring(0, colon), line.substring(colon + 1).trim());
        }
        String path = target.contains("?") ? target.substring(0, target.indexOf('?')) : target;
        String query = target.contains("?") ? target.substring(target.indexOf('?') + 1) : "";
        String body;
        int status = 200;
        switch (path) {
            case "/hello" -> body = "hello " + query.replace("name=", "");
            case "/slow" -> {
                sleep(1000);
                body = "done";
            }
            case "/agent" -> body = String.valueOf(headers.get("user-agent"));
            default -> {
                status = 404;
                body = "not found";
            }
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + (status == 200 ? " OK" : " Not Found") + "\r\nContent-Type: text/plain\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(bytes);
        out.flush();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        server.close();
    }
}
