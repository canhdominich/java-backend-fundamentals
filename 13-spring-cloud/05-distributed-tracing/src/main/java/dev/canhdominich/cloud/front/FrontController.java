package dev.canhdominich.cloud.front;

import dev.canhdominich.cloud.trace.TraceInfo;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class FrontController {

    private final Tracer tracer;
    private final RestClient inventory;

    public FrontController(Tracer tracer, RestClient.Builder builder, @Value("${inventory.url}") String inventoryUrl) {
        this.tracer = tracer;
        this.inventory = builder.baseUrl(inventoryUrl).build();
    }

    @GetMapping("/checkout")
    public String checkout(HttpServletRequest request) {
        return TraceInfo.describe("front", tracer, request) + "\n" + inventory.get().uri("/stock").retrieve().body(String.class);
    }
}
