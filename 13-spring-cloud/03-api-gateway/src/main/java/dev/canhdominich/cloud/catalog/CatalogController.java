package dev.canhdominich.cloud.catalog;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {

    private final AtomicInteger hits = new AtomicInteger();

    @Value("${instance.name:catalog}")
    private String name;

    @GetMapping("/products")
    public String products() {
        hits.incrementAndGet();
        return "products from " + name;
    }

    @GetMapping("/echo")
    public String echo(HttpServletRequest request) {
        return request.getRequestURI() + " | X-Gateway=" + request.getHeader("X-Gateway");
    }

    @GetMapping("/admin/hits")
    public String hits() {
        return String.valueOf(hits.get());
    }
}
