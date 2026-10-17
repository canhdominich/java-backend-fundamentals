package dev.canhdominich.intro.env;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EnvCheck {

    public enum Level {
        PASS, WARN, FAIL
    }

    public record Check(Level level, String text) {

        @Override
        public String toString() {
            return String.format("%-5s %s", level, text);
        }
    }

    public static void main(String[] args) {
        List<Check> checks = run();
        checks.forEach(System.out::println);
        System.exit(checks.stream().anyMatch(c -> c.level() == Level.FAIL) ? 1 : 0);
    }

    public static List<Check> run() {
        List<Check> checks = new ArrayList<>();
        int feature = Runtime.version().feature();
        checks.add(new Check(feature >= 17 ? Level.PASS : Level.FAIL, "JDK " + Runtime.version() + " (" + System.getProperty("java.vendor") + "), the course needs 17 or newer"));
        checks.add(new Check(Level.PASS, "default charset " + Charset.defaultCharset() + ", native encoding " + System.getProperty("native.encoding")));
        checks.add(new Check(Runtime.getRuntime().maxMemory() >= 256L * 1024 * 1024 ? Level.PASS : Level.WARN,
                "maximum heap " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB, processors " + Runtime.getRuntime().availableProcessors()));
        String maven = firstLine("mvn", "-v");
        checks.add(new Check(maven != null ? Level.PASS : Level.FAIL, maven != null ? maven : "Maven: the command mvn was not found on the PATH"));
        String docker = firstLine("docker", "version", "--format", "{{.Server.Version}}");
        checks.add(new Check(docker != null && !docker.isBlank() ? Level.PASS : Level.WARN,
                docker != null && !docker.isBlank() ? "Docker engine " + docker : "Docker is not available: the MySQL and Testcontainers labs need it"));
        boolean mysql = reachable("localhost", 3307);
        checks.add(new Check(mysql ? Level.PASS : Level.WARN, mysql ? "something listens on localhost:3307 (the MySQL of the JDBC labs)" : "nothing listens on localhost:3307: start it with the docker-compose.yml of the JDBC labs"));
        return checks;
    }

    static String firstLine(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            return new String(process.getInputStream().readAllBytes()).lines().findFirst().orElse("");
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    static boolean reachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
