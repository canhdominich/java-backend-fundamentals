package dev.canhdominich.spring.di;

import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class OptionalDemo {

    public static void main(String[] args) {
        run();
    }

    static class WithOptional {

        private final Optional<PaymentService> payment;

        WithOptional(Optional<PaymentService> payment) {
            this.payment = payment;
        }

        String describe() {
            return payment.map(PaymentService::name).orElse("none");
        }
    }

    static class WithProvider {

        private final ObjectProvider<PaymentService> payment;

        WithProvider(ObjectProvider<PaymentService> payment) {
            this.payment = payment;
        }

        String describe() {
            PaymentService found = payment.getIfAvailable();
            return found == null ? "none" : found.name();
        }
    }

    static class WithSetter {

        private PaymentService payment;

        @Autowired(required = false)
        void setPayment(PaymentService payment) {
            this.payment = payment;
        }

        String describe() {
            return payment == null ? "none" : payment.name();
        }
    }

    @Configuration
    static class NoPaymentConfig {

        @Bean
        WithOptional withOptional(Optional<PaymentService> payment) {
            return new WithOptional(payment);
        }

        @Bean
        WithProvider withProvider(ObjectProvider<PaymentService> payment) {
            return new WithProvider(payment);
        }

        @Bean
        WithSetter withSetter() {
            return new WithSetter();
        }
    }

    public static String[] run() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NoPaymentConfig.class)) {
            String[] results = {
                    context.getBean(WithOptional.class).describe(),
                    context.getBean(WithProvider.class).describe(),
                    context.getBean(WithSetter.class).describe()};
            System.out.println("no PaymentService bean at all: Optional=" + results[0] + ", ObjectProvider=" + results[1] + ", required=false setter=" + results[2]);
            return results;
        }
    }
}
