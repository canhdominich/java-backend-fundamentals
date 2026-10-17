package dev.canhdominich.cloud.inventory;

import brave.handler.SpanHandler;
import dev.canhdominich.cloud.trace.SpanCollector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryApplication {

    @Bean
    public SpanHandler spanCollector() {
        return new SpanCollector();
    }

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
