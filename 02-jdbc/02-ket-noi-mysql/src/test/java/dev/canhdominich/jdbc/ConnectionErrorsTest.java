package dev.canhdominich.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class ConnectionErrorsTest {

    private final DbConfig app = MySqlLab.appUser();

    @Test
    void wrongPasswordIsAccessDenied() {
        SQLException error = catchThrowableOfType(SQLException.class,
                () -> DriverManager.getConnection(app.url(), app.user(), "wrong"));

        assertThat(error.getSQLState()).isEqualTo("28000");
        assertThat(error.getErrorCode()).isEqualTo(1045);
    }

    @Test
    void closedPortIsCommunicationsLinkFailure() {
        String url = MySqlLab.jdbcUrl("shop").replace(":" + MySqlLab.mappedPort(), ":1");

        CommunicationsException error = catchThrowableOfType(CommunicationsException.class,
                () -> DriverManager.getConnection(url, app.user(), app.password()));

        assertThat(error.getSQLState()).isEqualTo("08S01");
        assertThat(error.getCause()).hasRootCauseInstanceOf(java.net.ConnectException.class);
    }

    @Test
    void unknownDatabaseIsReportedAsAccessDeniedForOrdinaryUser() {
        SQLException error = catchThrowableOfType(SQLException.class,
                () -> DriverManager.getConnection(MySqlLab.jdbcUrl("no_such_db"), app.user(), app.password()));

        assertThat(error.getErrorCode()).isEqualTo(1044);
    }

    @Test
    void unknownDatabaseIsReportedAsUnknownForAdministrator() {
        DbConfig root = MySqlLab.rootUser();

        SQLException error = catchThrowableOfType(SQLException.class,
                () -> DriverManager.getConnection(MySqlLab.jdbcUrl("no_such_db"), root.user(), root.password()));

        assertThat(error.getErrorCode()).isEqualTo(1049);
    }

    @Test
    void mistypedUrlPrefixIsNoSuitableDriver() {
        SQLException error = catchThrowableOfType(SQLException.class,
                () -> DriverManager.getConnection(app.url().replace("jdbc:mysql:", "jdbc:mysq:"), app.user(), app.password()));

        assertThat(error.getMessage()).startsWith("No suitable driver found");
    }
}
