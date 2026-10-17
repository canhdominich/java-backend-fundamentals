package dev.canhdominich.boot.ops;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("queue")
public class QueueHealthIndicator implements HealthIndicator {

    private static final int LIMIT = 1000;

    private final WorkQueue queue;

    public QueueHealthIndicator(WorkQueue queue) {
        this.queue = queue;
    }

    @Override
    public Health health() {
        int depth = queue.depth();
        Health.Builder builder = depth > LIMIT ? Health.down() : Health.up();
        return builder.withDetail("depth", depth).withDetail("limit", LIMIT).build();
    }
}
