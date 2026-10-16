package dev.canhdominich.servlet.intro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class RawHttp {

    private RawHttp() {
    }

    public static String exchange(int port, String request) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            try {
                int read;
                while ((read = socket.getInputStream().read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
            } catch (java.net.SocketTimeoutException e) {
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    public static void main(String[] args) throws Exception {
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            String request = "GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            System.out.println(">>> request");
            System.out.println(request.replace("\r", ""));
            System.out.println("<<< response");
            System.out.println(exchange(server.port(), request).replace("\r", ""));
        }
    }
}
