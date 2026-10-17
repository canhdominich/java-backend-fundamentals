package dev.canhdominich.spring.di;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

public class CollectionDemo {

    public static void main(String[] args) {
        run();
    }

    static class AllPayments {

        private final List<PaymentService> ordered;
        private final Map<String, PaymentService> byName;

        AllPayments(List<PaymentService> ordered, Map<String, PaymentService> byName) {
            this.ordered = ordered;
            this.byName = byName;
        }

        List<String> orderedNames() {
            return ordered.stream().map(PaymentService::name).toList();
        }

        List<String> keys() {
            return byName.keySet().stream().sorted().toList();
        }
    }

    @Configuration
    static class CollectionConfig {

        @Bean
        @Order(2)
        PaymentService paypal() {
            return new PaypalPayment();
        }

        @Bean
        @Order(1)
        PaymentService stripe() {
            return new StripePayment();
        }

        @Bean
        AllPayments allPayments(List<PaymentService> ordered, Map<String, PaymentService> byName) {
            return new AllPayments(ordered, byName);
        }
    }

    public static List<List<String>> run() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CollectionConfig.class)) {
            AllPayments all = context.getBean(AllPayments.class);
            System.out.println("List<PaymentService> in @Order: " + all.orderedNames());
            System.out.println("Map<String, PaymentService> keys: " + all.keys());
            return List.of(all.orderedNames(), all.keys());
        }
    }
}
