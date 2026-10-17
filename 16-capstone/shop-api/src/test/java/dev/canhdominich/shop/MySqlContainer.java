package dev.canhdominich.shop;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.mysql.MySQLContainer;

public final class MySqlContainer {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("shop");

    static {
        MYSQL.start();
    }

    private MySqlContainer() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
