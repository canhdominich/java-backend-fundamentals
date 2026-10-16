package dev.canhdominich.servlet.intro;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class HardenedErrorsTest {

    @Test
    void errorReportValveWithoutReportHidesMessageStackAndVersion() throws Exception {
        System.setProperty("lab.hardenErrors", "true");
        try (EmbeddedServer server = EmbeddedServer.start(0)) {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> boom = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/boom")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(boom.statusCode()).isEqualTo(500);
            assertThat(boom.body()).doesNotContain("hunter2").doesNotContain("IllegalStateException").doesNotContain("Apache Tomcat");
            HttpResponse<String> missing = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/nowhere")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(missing.statusCode()).isEqualTo(404);
            assertThat(missing.body()).doesNotContain("Apache Tomcat");
        } finally {
            System.clearProperty("lab.hardenErrors");
        }
    }
}
