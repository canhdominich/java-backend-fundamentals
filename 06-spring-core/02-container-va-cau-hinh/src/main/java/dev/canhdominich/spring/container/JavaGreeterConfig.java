package dev.canhdominich.spring.container;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaGreeterConfig {

    @Bean
    public Greeter greeter() {
        return new Greeter("hello from a @Bean method");
    }
}
