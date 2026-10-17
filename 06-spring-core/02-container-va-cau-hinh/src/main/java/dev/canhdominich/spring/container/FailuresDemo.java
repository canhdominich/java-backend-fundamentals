package dev.canhdominich.spring.container;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class FailuresDemo {

    public static void main(String[] args) {
        missingBean();
        circularConstructor();
        circularFields();
    }

    @Configuration
    static class MissingEngineConfig {

        @Bean
        Car car(Engine engine) {
            return new Car(engine);
        }
    }

    static class First {

        First(Second second) {
        }
    }

    static class Second {

        Second(First first) {
        }
    }

    @Configuration
    static class CircularConstructorConfig {

        @Bean
        First first(Second second) {
            return new First(second);
        }

        @Bean
        Second second(First first) {
            return new Second(first);
        }
    }

    static class FieldA {

        @Autowired
        FieldB other;
    }

    static class FieldB {

        @Autowired
        FieldA other;
    }

    @Configuration
    static class CircularFieldsConfig {

        @Bean
        FieldA fieldA() {
            return new FieldA();
        }

        @Bean
        FieldB fieldB() {
            return new FieldB();
        }
    }

    static String missingBean() {
        return start("a constructor needs an Engine that nobody declared", MissingEngineConfig.class);
    }

    static String circularConstructor() {
        return start("two beans need each other in their constructors", CircularConstructorConfig.class);
    }

    static String circularFields() {
        return start("two beans reference each other through @Autowired fields", CircularFieldsConfig.class);
    }

    private static String start(String label, Class<?> config) {
        System.out.println("-- " + label);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(config)) {
            String result = "started, " + context.getBeanDefinitionCount() + " definitions";
            System.out.println(result);
            return result;
        } catch (BeansException e) {
            Throwable cause = e;
            while (cause.getCause() != null && !(cause instanceof org.springframework.beans.factory.BeanCurrentlyInCreationException) && !(cause instanceof org.springframework.beans.factory.NoSuchBeanDefinitionException)) {
                cause = cause.getCause();
            }
            String result = "failed at startup: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            System.out.println(result);
            return result;
        }
    }
}
