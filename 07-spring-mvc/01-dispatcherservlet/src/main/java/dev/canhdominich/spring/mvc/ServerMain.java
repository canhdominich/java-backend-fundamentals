package dev.canhdominich.spring.mvc;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        EmbeddedServer server = EmbeddedServer.start(port, WebConfig.class);
        System.out.println("listening on http://127.0.0.1:" + server.port() + "/hello");
        Thread.currentThread().join();
    }
}
