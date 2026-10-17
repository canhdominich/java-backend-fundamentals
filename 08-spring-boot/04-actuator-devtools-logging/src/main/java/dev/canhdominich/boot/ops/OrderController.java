package dev.canhdominich.boot.ops;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);

    private final Counter created;
    private final WorkQueue queue;

    public OrderController(MeterRegistry registry, WorkQueue queue) {
        this.created = Counter.builder("orders.created").description("Orders accepted").register(registry);
        this.queue = queue;
    }

    @PostMapping("/orders")
    public String create() {
        created.increment();
        LOG.debug("order accepted, details for debugging");
        LOG.info("order accepted");
        return "ok";
    }

    @PostMapping("/queue")
    public String depth(@RequestParam int depth) {
        queue.depth(depth);
        return "depth=" + depth;
    }
}
