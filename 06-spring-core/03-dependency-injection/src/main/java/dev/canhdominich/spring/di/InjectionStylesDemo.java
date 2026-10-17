package dev.canhdominich.spring.di;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class InjectionStylesDemo {

    public static void main(String[] args) {
        run();
    }

    static class ConstructorOrder {

        private final PaymentService payment;

        ConstructorOrder(PaymentService payment) {
            this.payment = payment;
        }

        String place() {
            return payment.name();
        }
    }

    static class SetterOrder {

        private PaymentService payment;

        @Autowired
        void setPayment(PaymentService payment) {
            this.payment = payment;
        }

        String place() {
            return payment.name();
        }
    }

    static class FieldOrder {

        @Autowired
        private PaymentService payment;

        String place() {
            return payment.name();
        }
    }

    @Configuration
    static class StylesConfig {

        @Bean
        PaymentService payment() {
            return new PaypalPayment();
        }

        @Bean
        ConstructorOrder constructorOrder(PaymentService payment) {
            return new ConstructorOrder(payment);
        }

        @Bean
        SetterOrder setterOrder() {
            return new SetterOrder();
        }

        @Bean
        FieldOrder fieldOrder() {
            return new FieldOrder();
        }
    }

    public static String[] run() {
        String[] results = new String[5];
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(StylesConfig.class)) {
            results[0] = context.getBean(ConstructorOrder.class).place();
            results[1] = context.getBean(SetterOrder.class).place();
            results[2] = context.getBean(FieldOrder.class).place();
        }
        System.out.println("inside the container: constructor=" + results[0] + ", setter=" + results[1] + ", field=" + results[2]);
        results[3] = outsideTheContainer("setter", () -> new SetterOrder().place());
        results[4] = outsideTheContainer("field", () -> new FieldOrder().place());
        System.out.println("constructor injection outside a container: new ConstructorOrder() does not compile without a PaymentService");
        return results;
    }

    private static String outsideTheContainer(String style, java.util.function.Supplier<String> action) {
        try {
            return action.get();
        } catch (NullPointerException e) {
            System.out.println("outside the container, " + style + " injection: NullPointerException on first use");
            return "NullPointerException";
        }
    }
}
