package dev.canhdominich.boot.query;

import org.testcontainers.mysql.MySQLContainer;

final class MySqlLab {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("shop")
            .withUsername("app")
            .withPassword("app_pw");

    static {
        MYSQL.start();
    }

    private MySqlLab() {
    }

    static String url() {
        return MYSQL.getJdbcUrl();
    }

    static String user() {
        return "root";
    }

    static String password() {
        return MYSQL.getPassword();
    }
}
