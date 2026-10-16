package dev.canhdominich.jdbc.intro;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class WhatDriverSendsTest {

    @Test
    void connectingSendsInitStatementsAndPreparedValueTravelsInsideTheSqlText() throws SQLException {
        DbConfig app = MySqlLab.appUser();
        try (Connection connection = DriverManager.getConnection(app.url(), app.user(), app.password())) {
            Schema.reset(connection);
        }
        List<String> lines = WhatDriverSends.trace(app, MySqlLab.rootUser());

        assertThat(lines.get(0)).startsWith("Connect").contains("app@");
        assertThat(lines).anyMatch(line -> line.startsWith("Query") && line.contains("@@session.auto_increment_increment"));
        assertThat(lines).anyMatch(line -> line.contains("SET autocommit=1"));
        assertThat(lines).anyMatch(line -> line.contains("SELECT name FROM product WHERE sku = 'KB-01'"));
        assertThat(lines).noneMatch(line -> line.startsWith("Prepare") || line.startsWith("Execute"));
        assertThat(lines.get(lines.size() - 1)).startsWith("Quit");
    }
}
