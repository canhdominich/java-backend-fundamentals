package dev.canhdominich.servlet.intro;

import org.apache.catalina.LifecycleException;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws LifecycleException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        EmbeddedServer server = EmbeddedServer.start(port);
        System.out.println("Listening on http://localhost:" + server.port() + "/hello");
        server.tomcat().getServer().await();
    }
}
