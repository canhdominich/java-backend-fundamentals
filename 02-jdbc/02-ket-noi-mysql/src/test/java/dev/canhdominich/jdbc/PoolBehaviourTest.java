package dev.canhdominich.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PoolBehaviourTest {

    private final DbConfig app = MySqlLab.appUser();

    @Test
    void everyDirectConnectionIsANewServerConnection() throws SQLException {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            try (Connection connection = DriverManager.getConnection(app.url(), app.user(), app.password())) {
                ids.add(PoolVsDirect.connectionId(connection));
            }
        }

        assertThat(ids).hasSize(5);
    }

    @Test
    void closingAPooledConnectionReturnsItToThePool() throws SQLException {
        try (HikariDataSource dataSource = new HikariDataSource(config(2, 1000))) {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    ids.add(PoolVsDirect.connectionId(connection));
                }
            }

            assertThat(ids).hasSize(1);
            assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
        }
    }

    @Test
    void borrowingMoreThanThePoolSizeTimesOut() throws SQLException {
        try (HikariDataSource dataSource = new HikariDataSource(config(2, 500));
             Connection first = dataSource.getConnection();
             Connection second = dataSource.getConnection()) {

            SQLTransientConnectionException error = catchThrowableOfType(SQLTransientConnectionException.class,
                    dataSource::getConnection);

            assertThat(error.getMessage()).contains("request timed out after").contains("total=2, active=2, idle=0");
        }
    }

    private HikariConfig config(int maxSize, long timeoutMillis) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(app.url());
        config.setUsername(app.user());
        config.setPassword(app.password());
        config.setMaximumPoolSize(maxSize);
        config.setMinimumIdle(maxSize);
        config.setConnectionTimeout(timeoutMillis);
        return config;
    }
}
