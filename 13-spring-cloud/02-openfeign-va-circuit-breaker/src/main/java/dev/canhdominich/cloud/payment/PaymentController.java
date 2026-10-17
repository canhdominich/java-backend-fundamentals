package dev.canhdominich.cloud.payment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PaymentController {

    private final AtomicReference<String> mode = new AtomicReference<>("ok");
    private final AtomicInteger hits = new AtomicInteger();

    @GetMapping("/payments/{orderId}")
    public String pay(@PathVariable long orderId) throws InterruptedException {
        hits.incrementAndGet();
        switch (mode.get()) {
            case "slow" -> Thread.sleep(2000);
            case "hang" -> Thread.sleep(65000);
            case "error" -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "payment gateway down");
            default -> {
            }
        }
        if (orderId == 404) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no such order");
        }
        return "paid-" + orderId;
    }

    @PostMapping("/admin/mode/{mode}")
    public String setMode(@PathVariable String mode) {
        this.mode.set(mode);
        return mode;
    }

    @GetMapping("/admin/hits")
    public String hits() {
        return String.valueOf(hits.get());
    }

    @PostMapping("/admin/reset")
    public String reset() {
        hits.set(0);
        mode.set("ok");
        return "reset";
    }
}
