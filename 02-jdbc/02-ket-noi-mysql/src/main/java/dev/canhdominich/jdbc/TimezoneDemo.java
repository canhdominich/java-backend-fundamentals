package dev.canhdominich.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

public class TimezoneDemo {

    private static final Instant MOMENT = Instant.parse("2026-10-16T03:00:00Z");

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();

        System.out.println("JVM time zone : " + java.util.TimeZone.getDefault().getID());
        System.out.println("Stored moment : " + MOMENT + " (epoch " + MOMENT.getEpochSecond() + ")");
        for (String option : new String[]{"default", "connectionTimeZone=UTC", "serverTimezone=UTC", "serverTimezone=Foo/Bar",
                "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"}) {
            try {
                store(config, option);
            } catch (SQLException e) {
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                System.out.printf("%-24s FAILED %s: %s%n", option, root.getClass().getSimpleName(), root.getMessage());
            }
        }
    }

    static void store(DbConfig config, String option) throws SQLException {
        String url = option.equals("default") ? config.url() : config.url() + "?" + option;
        try (Connection connection = DriverManager.getConnection(url, config.user(), config.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS tz_demo");
            statement.execute("CREATE TABLE tz_demo (id INT PRIMARY KEY, created_at TIMESTAMP NOT NULL)");
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO tz_demo VALUES (1, ?)")) {
                insert.setTimestamp(1, Timestamp.from(MOMENT));
                insert.executeUpdate();
            }
            try (ResultSet rs = statement.executeQuery("SELECT CAST(created_at AS CHAR), UNIX_TIMESTAMP(created_at), @@session.time_zone FROM tz_demo")) {
                rs.next();
                long epoch = rs.getLong(2);
                System.out.printf("%-24s server shows %s, epoch %d, offset from truth %+d h, session time_zone %s%n",
                        option, rs.getString(1), epoch, (epoch - MOMENT.getEpochSecond()) / 3600, rs.getString(3));
            }
        }
    }
}
