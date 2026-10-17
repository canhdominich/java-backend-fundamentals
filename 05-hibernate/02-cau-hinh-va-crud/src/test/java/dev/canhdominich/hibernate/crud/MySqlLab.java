package dev.canhdominich.hibernate.crud;

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

    static DbConfig appUser() {
        return new DbConfig(MYSQL.getJdbcUrl(), "app", "app_pw");
    }

    static DbConfig rootUser() {
        return new DbConfig(MYSQL.getJdbcUrl(), "root", "app_pw");
    }

    static String jdbcUrl(String database) {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/" + database;
    }

    static int mappedPort() {
        return MYSQL.getMappedPort(3306);
    }
}
