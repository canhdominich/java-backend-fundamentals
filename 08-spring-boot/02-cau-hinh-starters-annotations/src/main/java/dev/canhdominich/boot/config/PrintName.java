package dev.canhdominich.boot.config;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class PrintName {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ConfigApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .properties("spring.main.banner-mode=off", "logging.level.root=OFF")
                .run(args)) {
            AppProperties properties = context.getBean(AppProperties.class);
            System.out.println("name=" + properties.name() + " maxSize=" + properties.maxSize());
        }
    }
}
