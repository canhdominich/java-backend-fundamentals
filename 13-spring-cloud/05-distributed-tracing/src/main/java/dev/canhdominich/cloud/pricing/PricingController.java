package dev.canhdominich.cloud.pricing;

import dev.canhdominich.cloud.trace.TraceInfo;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PricingController {

    private final Tracer tracer;

    public PricingController(Tracer tracer) {
        this.tracer = tracer;
    }

    @GetMapping("/price")
    public String price(HttpServletRequest request) {
        return TraceInfo.describe("pricing", tracer, request);
    }

    @GetMapping("/async")
    public String async() throws Exception {
        String inRequest = MDC.get("traceId");
        String inOtherThread = CompletableFuture.supplyAsync(() -> String.valueOf(MDC.get("traceId"))).get();
        return "request thread traceId=" + inRequest + " other thread traceId=" + inOtherThread;
    }
}
