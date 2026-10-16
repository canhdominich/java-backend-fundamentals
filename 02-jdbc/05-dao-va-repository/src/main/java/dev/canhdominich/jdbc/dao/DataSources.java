package dev.canhdominich.jdbc.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

final class DataSources {

    private DataSources() {
    }

    static HikariDataSource create(DbConfig config, int maxPoolSize) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.user());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(maxPoolSize);
        hikari.setPoolName("dao-lab");
        return new HikariDataSource(hikari);
    }
}
