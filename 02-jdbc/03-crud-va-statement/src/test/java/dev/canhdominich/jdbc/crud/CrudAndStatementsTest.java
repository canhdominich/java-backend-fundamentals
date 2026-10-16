package dev.canhdominich.jdbc.crud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrudAndStatementsTest {

    private final DbConfig app = MySqlLab.appUser();

    @BeforeEach
    void resetSchema() throws SQLException {
        try (Connection connection = Db.open(app)) {
            Db.resetSchema(connection);
        }
    }

    @Test
    void crudRoundTripReportsGeneratedKeyAndAffectedRows() throws SQLException {
        try (Connection connection = Db.open(app)) {
            long id = CrudDemo.insert(connection, "KB-01", "Keyboard", new BigDecimal("49.90"), 12);
            assertThat(id).isPositive();
            assertThat(CrudDemo.findById(connection, id)).get().extracting(Product::name).isEqualTo("Keyboard");
            assertThat(CrudDemo.updatePrice(connection, id, new BigDecimal("59.90"))).isEqualTo(1);
            assertThat(CrudDemo.searchByMinPrice(connection, new BigDecimal("20.00"))).extracting(Product::price).containsExactly(new BigDecimal("59.90"));
            assertThat(CrudDemo.delete(connection, id)).isEqualTo(1);
            assertThat(CrudDemo.delete(connection, id)).isZero();
            assertThat(CrudDemo.findById(connection, id)).isEmpty();
        }
    }

    @Test
    void duplicateSkuViolatesUniqueConstraint() throws SQLException {
        try (Connection connection = Db.open(app)) {
            CrudDemo.insert(connection, "KB-01", "Keyboard", new BigDecimal("49.90"), 12);
            assertThatThrownBy(() -> CrudDemo.insert(connection, "KB-01", "Other", new BigDecimal("1.00"), 1))
                    .isInstanceOf(SQLIntegrityConstraintViolationException.class);
        }
    }

    @Test
    void concatenatedLoginIsInjectableButPreparedLoginIsNot() throws SQLException {
        try (Connection connection = Db.open(app)) {
            assertThat(SqlInjectionDemo.vulnerableLogin(connection, "an", "wrong")).isEqualTo("denied");
            assertThat(SqlInjectionDemo.vulnerableLogin(connection, "admin' -- ", "anything")).isEqualTo("LOGGED_IN as ADMIN");
            assertThat(SqlInjectionDemo.vulnerableLogin(connection, "nobody", "x' OR '1'='1")).startsWith("LOGGED_IN");
            assertThat(SqlInjectionDemo.safeLogin(connection, "admin' -- ", "anything")).isEqualTo("denied");
            assertThat(SqlInjectionDemo.safeLogin(connection, "nobody", "x' OR '1'='1")).isEqualTo("denied");
            assertThat(SqlInjectionDemo.safeLogin(connection, "an", "hash-an")).isEqualTo("LOGGED_IN as USER");
        }
    }

    @Test
    void stackedQueryNeedsAllowMultiQueriesToDoDamage() throws SQLException {
        assertThat(SqlInjectionDemo.stacked(app, "default URL")).contains("rejected SQLSyntaxErrorException").contains("audit_log exists = true");
        assertThat(SqlInjectionDemo.stacked(app, "allowMultiQueries=true")).contains("query ran").contains("audit_log exists = false");
    }

    @Test
    void affectedRowsCountsMatchedRowsUnlessUseAffectedRowsIsSet() throws SQLException {
        try (Connection connection = Db.open(app)) {
            CrudDemo.insert(connection, "KB-01", "Keyboard", new BigDecimal("49.90"), 12);
            assertThat(AffectedRows.setStock(connection, 12)).isEqualTo(1);
        }
        try (Connection connection = Db.open(app, "useAffectedRows=true")) {
            assertThat(AffectedRows.setStock(connection, 12)).isZero();
            assertThat(AffectedRows.setStock(connection, 13)).isEqualTo(1);
        }
    }

    @Test
    void placeholderInOrderByDoesNotSortAndWhitelistDoes() throws SQLException {
        try (Connection connection = Db.open(app); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('A', 'Charger', 30.00, 5), ('B', 'Adapter', 10.00, 9), ('C', 'Battery', 20.00, 1)");
            assertThat(OrderByAndIn.names(connection, "SELECT name FROM product ORDER BY ?", "price")).containsExactly("Charger", "Adapter", "Battery");
            assertThat(OrderByAndIn.names(connection, "SELECT name FROM product ORDER BY " + OrderByAndIn.sortColumn("price"), null)).containsExactly("Adapter", "Battery", "Charger");
            assertThatThrownBy(() -> OrderByAndIn.sortColumn("price; DROP TABLE product")).isInstanceOf(IllegalArgumentException.class);
            assertThat(OrderByAndIn.findBySkus(connection, List.of("A", "C"))).containsExactly("Battery", "Charger");
        }
    }

    @Test
    void callableStatementReadsOutParameter() throws SQLException {
        try (Connection connection = Db.open(app); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('A', 'Charger', 30.00, 5), ('B', 'Adapter', 10.00, 9)");
            statement.execute("DROP PROCEDURE IF EXISTS count_products");
            statement.execute("CREATE PROCEDURE count_products(IN min_price DECIMAL(10,2), OUT total INT) SELECT COUNT(*) INTO total FROM product WHERE price >= min_price");
            assertThat(StoredProcedureDemo.countAtLeast(connection, new BigDecimal("15.00"))).isEqualTo(1);
            assertThat(StoredProcedureDemo.countAtLeast(connection, new BigDecimal("5.00"))).isEqualTo(2);
        }
    }

    @Test
    void streamingResultSetBlocksOtherStatementsOnTheSameConnection() throws SQLException {
        try (Connection connection = Db.open(app); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('A', 'Charger', 30.00, 5), ('B', 'Adapter', 10.00, 9)");
            try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM product", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                ps.setFetchSize(Integer.MIN_VALUE);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertThatThrownBy(() -> {
                        try (Statement other = connection.createStatement()) {
                            other.executeQuery("SELECT 1");
                        }
                    }).isInstanceOf(SQLException.class).hasMessageContaining("Streaming result set");
                    while (rs.next()) {
                    }
                }
            }
            try (ResultSet rs = statement.executeQuery("SELECT 1")) {
                assertThat(rs.next()).isTrue();
            }
        }
    }
}
