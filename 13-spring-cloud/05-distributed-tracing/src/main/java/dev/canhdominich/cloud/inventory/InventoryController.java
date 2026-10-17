package dev.canhdominich.cloud.inventory;

import dev.canhdominich.cloud.trace.TraceInfo;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class InventoryController {

    private final Tracer tracer;
    private final RestClient pricing;

    public InventoryController(Tracer tracer, RestClient.Builder builder, @Value("${pricing.url}") String pricingUrl) {
        this.tracer = tracer;
        this.pricing = builder.baseUrl(pricingUrl).build();
    }

    @GetMapping("/stock")
    public String stock(HttpServletRequest request) {
        return TraceInfo.describe("inventory", tracer, request) + "\n" + pricing.get().uri("/price").retrieve().body(String.class);
    }
}
