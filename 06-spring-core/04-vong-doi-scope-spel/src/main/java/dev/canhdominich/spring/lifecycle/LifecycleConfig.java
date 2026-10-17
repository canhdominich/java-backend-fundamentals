package dev.canhdominich.spring.lifecycle;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LifecycleConfig {

    @Bean
    public static Tracer tracer() {
        return new Tracer();
    }

    @Bean
    public Engine engine() {
        return new Engine();
    }

    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public Probe probe() {
        return new Probe();
    }
}
