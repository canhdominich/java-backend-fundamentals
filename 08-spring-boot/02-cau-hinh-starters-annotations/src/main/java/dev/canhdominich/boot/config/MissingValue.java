package dev.canhdominich.boot.config;

import org.springframework.beans.factory.annotation.Value;

public class MissingValue {

    @Value("${app.missing}")
    String missing;
}
