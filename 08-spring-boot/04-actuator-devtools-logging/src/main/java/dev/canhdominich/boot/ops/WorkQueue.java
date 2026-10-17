package dev.canhdominich.boot.ops;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class WorkQueue {

    private final AtomicInteger depth = new AtomicInteger();

    public int depth() {
        return depth.get();
    }

    public void depth(int value) {
        depth.set(value);
    }
}
