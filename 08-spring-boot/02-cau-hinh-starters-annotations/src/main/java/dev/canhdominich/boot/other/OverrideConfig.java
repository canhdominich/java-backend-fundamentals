package dev.canhdominich.boot.other;

import dev.canhdominich.boot.starter.GreetingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OverrideConfig {

    @Bean
    public GreetingService greetingService() {
        return new GreetingService("custom");
    }
}
