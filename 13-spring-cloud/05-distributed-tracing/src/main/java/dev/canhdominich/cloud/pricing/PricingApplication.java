package dev.canhdominich.cloud.pricing;

import brave.handler.SpanHandler;
import dev.canhdominich.cloud.trace.SpanCollector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PricingApplication {

    @Bean
    public SpanHandler spanCollector() {
        return new SpanCollector();
    }

    public static void main(String[] args) {
        SpringApplication.run(PricingApplication.class, args);
    }
}
