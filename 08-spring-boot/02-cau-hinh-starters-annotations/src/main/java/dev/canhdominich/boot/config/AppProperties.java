package dev.canhdominich.boot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String name,
        @Min(1) int maxSize,
        Duration timeout,
        List<String> admins,
        @Valid Mail mail) {

    public record Mail(String host, int port) {
    }
}
