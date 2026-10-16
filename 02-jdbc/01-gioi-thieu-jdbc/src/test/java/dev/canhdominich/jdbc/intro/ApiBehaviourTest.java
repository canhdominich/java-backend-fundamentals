package dev.canhdominich.jdbc.intro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiBehaviourTest {

    private final DbConfig app = MySqlLab.appUser();

    @BeforeEach
    void resetSchema() throws SQLException {
        try (Connection connection = open()) {
            Schema.reset(connection);
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(app.url(), app.user(), app.password());
    }

    @Test
    void driverImplementationClassesAreTheVendorOnes() throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement("SELECT id, price FROM product");
             ResultSet rs = ps.executeQuery()) {
            assertThat(connection.getClass().getName()).isEqualTo("com.mysql.cj.jdbc.ConnectionImpl");
            assertThat(ps.getClass().getName()).isEqualTo("com.mysql.cj.jdbc.ClientPreparedStatement");
            assertThat(rs.getMetaData().getColumnClassName(2)).isEqualTo("java.math.BigDecimal");
            assertThat(connection.getMetaData().getJDBCMajorVersion()).isEqualTo(4);
            assertThat(connection.getAutoCommit()).isTrue();
        }
    }

    @Test
    void getIntOnNullReturnsZeroAndOnlyWasNullTellsTheTruth() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT stock FROM product WHERE sku = 'MS-02'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("stock")).isZero();
            assertThat(rs.wasNull()).isTrue();
            assertThat(rs.getObject("stock", Integer.class)).isNull();
        }
    }

    @Test
    void closingTheConnectionClosesStatementAndResultSet() throws SQLException {
        Connection connection = open();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT 1");
        connection.close();
        assertThat(statement.isClosed()).isTrue();
        assertThat(rs.isClosed()).isTrue();
    }

    @Test
    void exceptionsCarrySqlStateAndVendorCode() throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement("INSERT INTO product(sku, name, price) VALUES (?, ?, ?)")) {
            ps.setString(1, "KB-01");
            ps.setString(2, "Duplicate");
            ps.setBigDecimal(3, new BigDecimal("10.00"));
            assertThatThrownBy(ps::executeUpdate)
                    .isInstanceOf(SQLIntegrityConstraintViolationException.class)
                    .satisfies(e -> {
                        SQLException sql = (SQLException) e;
                        assertThat(sql.getSQLState()).isEqualTo("23000");
                        assertThat(sql.getErrorCode()).isEqualTo(1062);
                    });
            try (Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeQuery("SELECT * FROM missing_table"))
                        .isInstanceOf(SQLSyntaxErrorException.class)
                        .satisfies(e -> assertThat(((SQLException) e).getErrorCode()).isEqualTo(1146));
                assertThatThrownBy(() -> statement.executeQuery("SELEC 1"))
                        .isExactlyInstanceOf(SQLException.class)
                        .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("S1009"));
            }
        }
    }
}
