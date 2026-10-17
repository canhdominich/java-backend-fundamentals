package dev.canhdominich.spring.lifecycle;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

public class ScopeDemo {

    public static void main(String[] args) {
        run();
    }

    public static class Shared {

        private static final AtomicInteger CREATED = new AtomicInteger();
        private final int id = CREATED.incrementAndGet();

        public int id() {
            return id;
        }

        @PreDestroy
        void bye() {
            Events.record("destroying singleton #" + id);
        }
    }

    public static class PerUse {

        private static final AtomicInteger CREATED = new AtomicInteger();
        private final int id = CREATED.incrementAndGet();

        public int id() {
            return id;
        }

        @PreDestroy
        void bye() {
            Events.record("destroying prototype #" + id);
        }
    }

    @Configuration
    static class ScopeConfig {

        @Bean
        Shared shared() {
            return new Shared();
        }

        @Bean
        @Scope("prototype")
        PerUse perUse() {
            return new PerUse();
        }
    }

    public static List<String> run() {
        Events.clear();
        List<String> lines;
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScopeConfig.class)) {
            int s1 = context.getBean(Shared.class).id();
            int s2 = context.getBean(Shared.class).id();
            int p1 = context.getBean(PerUse.class).id();
            int p2 = context.getBean(PerUse.class).id();
            lines = List.of("singleton ids: " + s1 + ", " + s2, "prototype ids: " + p1 + ", " + p2);
            lines.forEach(System.out::println);
            System.out.println("-- closing the context");
        }
        System.out.println("destroy callbacks that ran: " + Events.all());
        return List.of(lines.get(0), lines.get(1), "destroyed: " + Events.all());
    }
}
