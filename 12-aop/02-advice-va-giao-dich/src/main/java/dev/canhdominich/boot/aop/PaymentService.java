package dev.canhdominich.boot.aop;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final AtomicInteger priceLookups = new AtomicInteger();

    @Traced
    public String charge(int amount) {
        Events.record("body: charge " + amount);
        if (amount <= 0) {
            throw new IllegalStateException("amount must be positive");
        }
        return "charged " + amount;
    }

    @Capped
    public String receiptNumber() {
        return "receipt-12345";
    }

    @Cached
    public int priceOf(String item) {
        priceLookups.incrementAndGet();
        return item.length() * 1000;
    }

    public int lookups() {
        return priceLookups.get();
    }
}
