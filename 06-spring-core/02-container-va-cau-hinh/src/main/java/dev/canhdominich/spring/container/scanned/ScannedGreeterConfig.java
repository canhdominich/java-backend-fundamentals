package dev.canhdominich.spring.container.scanned;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ScannedGreeterConfig.class)
public class ScannedGreeterConfig {
}
