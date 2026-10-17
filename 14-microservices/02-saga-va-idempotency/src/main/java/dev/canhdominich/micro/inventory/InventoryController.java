package dev.canhdominich.micro.inventory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class InventoryController {

    private final StockService stock;
    private final AtomicInteger delayMillis = new AtomicInteger();
    private final AtomicInteger delayedCalls = new AtomicInteger();
    private final AtomicBoolean releaseDown = new AtomicBoolean();

    public InventoryController(StockService stock) {
        this.stock = stock;
    }

    @PostMapping("/reserve")
    public String reserve(@RequestParam String sku, @RequestParam int quantity, @RequestParam(required = false) String key) throws InterruptedException {
        String result = stock.reserve(sku, quantity, key);
        if (delayedCalls.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            Thread.sleep(delayMillis.get());
        }
        return result;
    }

    @PostMapping("/release")
    public String release(@RequestParam String key) {
        if (releaseDown.get()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "release is down");
        }
        return stock.release(key);
    }

    @GetMapping("/stock/{sku}")
    public String stock(@PathVariable String sku) {
        return String.valueOf(stock.stock(sku));
    }

    @PostMapping("/admin/delay")
    public String delay(@RequestParam int millis, @RequestParam int calls) {
        delayMillis.set(millis);
        delayedCalls.set(calls);
        return "delay " + millis + " for " + calls + " calls";
    }

    @PostMapping("/admin/release-down")
    public String releaseDown(@RequestParam boolean value) {
        releaseDown.set(value);
        return "release down " + value;
    }
}
