package dev.canhdominich.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimezoneAndLimitsTest {

    private final DbConfig app = MySqlLab.appUser();
    private TimeZone original;

    @BeforeEach
    void useVietnamTimeZone() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(original);
    }

    @Test
    void defaultConnectionStoresTheLocalClockAsIfItWereServerTime() throws SQLException {
        assertThat(storedClock(app.url())).isEqualTo("2026-10-16 10:00:00");
    }

    @Test
    void explicitConnectionTimeZoneStoresTheRealInstant() throws SQLException {
        assertThat(storedClock(app.url() + "?connectionTimeZone=UTC")).isEqualTo("2026-10-16 03:00:00");
    }

    @Test
    void misspelledOptionIsSilentlyIgnoredAndTlsStaysOn() throws SQLException {
        try (Connection connection = DriverManager.getConnection(app.url() + "?useSSl=false", app.user(), app.password());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
            rs.next();
            assertThat(rs.getString(2)).isNotBlank();
        }
    }

    @Test
    void legacyUseSslFalseStillDisablesTls() throws SQLException {
        try (Connection connection = DriverManager.getConnection(app.url() + "?useSSL=false", app.user(), app.password());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
            rs.next();
            assertThat(rs.getString(2)).isEmpty();
        }
    }

    @Test
    void serverRefusesConnectionsBeyondMaxConnections() throws SQLException {
        DbConfig root = MySqlLab.rootUser();
        int limit = 8;
        try (Connection admin = DriverManager.getConnection(root.url(), root.user(), root.password());
             Statement statement = admin.createStatement()) {
            statement.execute("SET GLOBAL max_connections = " + limit);
        }
        List<Connection> open = new ArrayList<>();
        SQLException refused = null;
        try {
            for (int i = 0; i < limit + 2; i++) {
                open.add(DriverManager.getConnection(app.url(), app.user(), app.password()));
            }
        } catch (SQLException e) {
            refused = e;
        } finally {
            for (Connection connection : open) {
                connection.close();
            }
            try (Connection admin = DriverManager.getConnection(root.url(), root.user(), root.password());
                 Statement statement = admin.createStatement()) {
                statement.execute("SET GLOBAL max_connections = 151");
            }
        }

        assertThat((Throwable) refused).as("server refused a connection").isNotNull();
        assertThat(refused.getSQLState()).isEqualTo("08004");
        assertThat(refused.getErrorCode()).isEqualTo(1040);
        assertThat(refused.getMessage()).contains("Too many connections");
    }

    private String storedClock(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, app.user(), app.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS tz_check");
            statement.execute("CREATE TABLE tz_check (created_at TIMESTAMP NOT NULL)");
            try (var insert = connection.prepareStatement("INSERT INTO tz_check VALUES (?)")) {
                insert.setTimestamp(1, java.sql.Timestamp.from(java.time.Instant.parse("2026-10-16T03:00:00Z")));
                insert.executeUpdate();
            }
            try (ResultSet rs = statement.executeQuery("SELECT CAST(created_at AS CHAR) FROM tz_check")) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
