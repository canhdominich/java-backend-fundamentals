package dev.canhdominich.spring.di;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

public class ValueDemo {

    public static void main(String[] args) {
        run();
        missing();
        strict();
    }

    static class Settings {

        @Value("${app.name}")
        String name;

        @Value("${app.max-size:100}")
        int maxSize;

        @Value("${app.owner}")
        String owner;
    }

    static class BrokenSettings {

        @Value("${app.missing}")
        String missing;
    }

    @Configuration
    @PropertySource("classpath:app.properties")
    static class SettingsConfig {

        @Bean
        Settings settings() {
            return new Settings();
        }
    }

    @Configuration
    @PropertySource("classpath:app.properties")
    static class BrokenConfig {

        @Bean
        BrokenSettings brokenSettings() {
            return new BrokenSettings();
        }
    }

    public static String run() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SettingsConfig.class)) {
            Settings settings = context.getBean(Settings.class);
            String line = "name=" + settings.name + ", owner=" + settings.owner + ", maxSize=" + settings.maxSize + " (not in the file, default used)";
            System.out.println(line);
            return line;
        }
    }

    public static String missing() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BrokenConfig.class)) {
            String value = context.getBean(BrokenSettings.class).missing;
            System.out.println("placeholder without a value and without a default: context started, field = \"" + value + "\"");
            return value;
        } catch (BeansException e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            System.out.println("placeholder without a value and without a default: " + message);
            return message;
        }
    }

    @Configuration
    @PropertySource("classpath:app.properties")
    static class StrictConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        BrokenSettings brokenSettings() {
            return new BrokenSettings();
        }
    }

    public static String strict() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(StrictConfig.class)) {
            return "started, field = " + context.getBean(BrokenSettings.class).missing;
        } catch (BeansException e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            System.out.println("with PropertySourcesPlaceholderConfigurer: " + message);
            return message;
        }
    }
}
