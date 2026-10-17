package dev.canhdominich.boot.broken;

import dev.canhdominich.boot.query.Course;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = Course.class)
@EnableJpaRepositories(basePackageClasses = BrokenRepository.class)
public class BrokenApplication {
}
