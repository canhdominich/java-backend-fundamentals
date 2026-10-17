package dev.canhdominich.boot.aop;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final String prefix;

    public OrderService() {
        this.prefix = String.valueOf("order-");
    }

    @Audited
    public String place(String item) {
        Events.record("body: place " + item);
        return secret(item);
    }

    public String cancel(String item) {
        Events.record("body: cancel " + item);
        return "cancelled " + item;
    }

    public String outer(String item) {
        Events.record("body: outer " + item);
        return this.place(item);
    }

    public final String receipt(String item) {
        Events.record("body: receipt " + item);
        return prefix + item;
    }

    private String secret(String item) {
        Events.record("body: private secret " + item);
        return prefix + item;
    }
}
