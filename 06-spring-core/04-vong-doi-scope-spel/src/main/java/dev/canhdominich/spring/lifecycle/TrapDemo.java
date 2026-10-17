package dev.canhdominich.spring.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

public class TrapDemo {

    public static void main(String[] args) {
        run();
    }

    public static class Builder {

        private static final AtomicInteger CREATED = new AtomicInteger();
        private final int id = CREATED.incrementAndGet();

        public int id() {
            return id;
        }
    }

    public static class DirectService {

        private final Builder builder;

        public DirectService(Builder builder) {
            this.builder = builder;
        }

        public int builderId() {
            return builder.id();
        }
    }

    public static class ProviderService {

        private final ObjectProvider<Builder> builders;

        public ProviderService(ObjectProvider<Builder> builders) {
            this.builders = builders;
        }

        public int builderId() {
            return builders.getObject().id();
        }
    }

    public static class ProxiedBuilder {

        private static final AtomicInteger CREATED = new AtomicInteger();
        private final int id = CREATED.incrementAndGet();

        public int id() {
            return id;
        }
    }

    public static class ProxyService {

        private final ProxiedBuilder builder;

        public ProxyService(ProxiedBuilder builder) {
            this.builder = builder;
        }

        public int builderId() {
            return builder.id();
        }
    }

    @Configuration
    static class TrapConfig {

        @Bean
        @Scope("prototype")
        Builder builder() {
            return new Builder();
        }

        @Bean
        @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
        ProxiedBuilder proxiedBuilder() {
            return new ProxiedBuilder();
        }

        @Bean
        DirectService directService(Builder builder) {
            return new DirectService(builder);
        }

        @Bean
        ProviderService providerService(ObjectProvider<Builder> builders) {
            return new ProviderService(builders);
        }

        @Bean
        ProxyService proxyService(ProxiedBuilder builder) {
            return new ProxyService(builder);
        }
    }

    public static List<String> run() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TrapConfig.class)) {
            DirectService direct = context.getBean(DirectService.class);
            ProviderService provider = context.getBean(ProviderService.class);
            ProxyService proxy = context.getBean(ProxyService.class);
            List<String> lines = new ArrayList<>();
            lines.add("prototype injected directly into a singleton, three calls: " + ids(direct::builderId));
            lines.add("ObjectProvider.getObject() per call, three calls: " + ids(provider::builderId));
            lines.add("scoped proxy (proxyMode = TARGET_CLASS), three calls: " + ids(proxy::builderId));
            lines.forEach(System.out::println);
            return lines;
        }
    }

    private static String ids(java.util.function.IntSupplier call) {
        return call.getAsInt() + ", " + call.getAsInt() + ", " + call.getAsInt();
    }
}
