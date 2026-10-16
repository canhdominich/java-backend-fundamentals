package dev.canhdominich.jdbc.intro;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class ApiTour {

    private ApiTour() {
    }

    public static void main(String[] args) throws SQLException {
        DbConfig config = DbConfig.fromEnvironment();
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            Schema.reset(connection);
            implementations(connection);
            capabilities(connection);
            nullHandling(connection);
            closeCascade(config);
            exceptionAnatomy(connection);
        }
    }

    static void implementations(Connection connection) throws SQLException {
        System.out.println("-- implementations");
        System.out.println("Connection        " + connection.getClass().getName());
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, price FROM product")) {
            System.out.println("PreparedStatement " + ps.getClass().getName());
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("ResultSet         " + rs.getClass().getName());
                System.out.println("ResultSetMetaData " + rs.getMetaData().getClass().getName());
                System.out.println("price column type " + rs.getMetaData().getColumnTypeName(2) + " -> " + rs.getMetaData().getColumnClassName(2));
            }
        }
    }

    static void capabilities(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        System.out.println("-- capabilities");
        System.out.println("product        " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        System.out.println("driver         " + meta.getDriverName() + " " + meta.getDriverVersion());
        System.out.println("JDBC version   " + meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
        System.out.println("transactions   " + meta.supportsTransactions());
        System.out.println("batch updates  " + meta.supportsBatchUpdates());
        System.out.println("isolation      " + connection.getTransactionIsolation() + " (REPEATABLE_READ=" + Connection.TRANSACTION_REPEATABLE_READ + ")");
        System.out.println("autoCommit     " + connection.getAutoCommit());
    }

    static void nullHandling(Connection connection) throws SQLException {
        System.out.println("-- null handling for the Mouse row (stock is NULL)");
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT stock FROM product WHERE sku = 'MS-02'")) {
            rs.next();
            int asInt = rs.getInt("stock");
            System.out.println("getInt     = " + asInt + ", wasNull = " + rs.wasNull());
            System.out.println("getObject  = " + rs.getObject("stock"));
            System.out.println("getObject(Integer.class) = " + rs.getObject("stock", Integer.class));
        }
    }

    static void closeCascade(DbConfig config) throws SQLException {
        System.out.println("-- closing the connection closes what it created");
        Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT 1");
        connection.close();
        System.out.println("statement.isClosed = " + statement.isClosed());
        System.out.println("resultSet.isClosed = " + rs.isClosed());
    }

    static void exceptionAnatomy(Connection connection) throws SQLException {
        System.out.println("-- anatomy of an SQLException");
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO product(sku, name, price) VALUES (?, ?, ?)")) {
            ps.setString(1, "KB-01");
            ps.setString(2, "Duplicate keyboard");
            ps.setBigDecimal(3, new java.math.BigDecimal("10.00"));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("class      = " + e.getClass().getName());
            System.out.println("superclass = " + e.getClass().getSuperclass().getName());
            System.out.println("SQLState   = " + e.getSQLState());
            System.out.println("errorCode  = " + e.getErrorCode());
            System.out.println("message    = " + e.getMessage());
        }
        for (String sql : new String[]{"SELECT * FROM missing_table", "SELEC 1"}) {
            try (Statement statement = connection.createStatement()) {
                statement.executeQuery(sql);
            } catch (SQLException e) {
                System.out.println(sql + " -> " + e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " message=" + e.getMessage());
            }
        }
    }
}
