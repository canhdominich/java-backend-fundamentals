package dev.canhdominich.spring.di;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

public class ResolutionDemo {

    public static void main(String[] args) {
        ambiguous();
        withQualifier();
        withPrimary();
        qualifierBeatsPrimary();
    }

    static class Consumer {

        private final PaymentService payment;

        Consumer(PaymentService payment) {
            this.payment = payment;
        }

        String uses() {
            return payment.name();
        }
    }

    static class QualifiedConsumer {

        private final PaymentService payment;

        QualifiedConsumer(@Qualifier("stripe") PaymentService payment) {
            this.payment = payment;
        }

        String uses() {
            return payment.name();
        }
    }

    @Configuration
    static class TwoBeansConfig {

        @Bean
        PaymentService paypal() {
            return new PaypalPayment();
        }

        @Bean
        PaymentService stripe() {
            return new StripePayment();
        }

        @Bean
        Consumer consumer(PaymentService payment) {
            return new Consumer(payment);
        }
    }

    @Configuration
    static class QualifierConfig {

        @Bean
        PaymentService paypal() {
            return new PaypalPayment();
        }

        @Bean
        PaymentService stripe() {
            return new StripePayment();
        }

        @Bean
        QualifiedConsumer qualifiedConsumer(@Qualifier("stripe") PaymentService payment) {
            return new QualifiedConsumer(payment);
        }
    }

    @Configuration
    static class PrimaryConfig {

        @Bean
        @Primary
        PaymentService paypal() {
            return new PaypalPayment();
        }

        @Bean
        PaymentService stripe() {
            return new StripePayment();
        }

        @Bean
        Consumer consumer(PaymentService payment) {
            return new Consumer(payment);
        }

        @Bean
        QualifiedConsumer qualifiedConsumer(@Qualifier("stripe") PaymentService payment) {
            return new QualifiedConsumer(payment);
        }
    }

    static String ambiguous() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TwoBeansConfig.class)) {
            return "started";
        } catch (BeansException e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            System.out.println("two candidates, nothing to choose with: " + message);
            return message;
        }
    }

    static String withQualifier() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(QualifierConfig.class)) {
            String uses = context.getBean(QualifiedConsumer.class).uses();
            System.out.println("@Qualifier(\"stripe\") selects: " + uses);
            return uses;
        }
    }

    static String withPrimary() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PrimaryConfig.class)) {
            String uses = context.getBean(Consumer.class).uses();
            System.out.println("@Primary on paypal, plain parameter selects: " + uses);
            return uses;
        }
    }

    static String qualifierBeatsPrimary() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PrimaryConfig.class)) {
            String uses = context.getBean(QualifiedConsumer.class).uses();
            System.out.println("@Primary on paypal and @Qualifier(\"stripe\") on the parameter selects: " + uses);
            return uses;
        }
    }
}
