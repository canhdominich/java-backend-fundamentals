package dev.canhdominich.shop.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        if (ttl == null) {
            ttl = Duration.ofHours(1);
        }
    }
}
