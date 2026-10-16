package dev.canhdominich.jdbc.crud;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public final class StoredProcedureDemo {

    private StoredProcedureDemo() {
    }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = Db.open(DbConfig.fromEnvironment())) {
            Db.resetSchema(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('A', 'Charger', 30.00, 5), ('B', 'Adapter', 10.00, 9)");
                statement.execute("DROP PROCEDURE IF EXISTS count_products");
                statement.execute("CREATE PROCEDURE count_products(IN min_price DECIMAL(10,2), OUT total INT) SELECT COUNT(*) INTO total FROM product WHERE price >= min_price");
            }
            System.out.println("count_products(15.00) = " + countAtLeast(connection, new BigDecimal("15.00")));
            System.out.println("count_products(5.00)  = " + countAtLeast(connection, new BigDecimal("5.00")));
        }
    }

    static int countAtLeast(Connection connection, BigDecimal minPrice) throws SQLException {
        try (CallableStatement call = connection.prepareCall("{call count_products(?, ?)}")) {
            call.setBigDecimal(1, minPrice);
            call.registerOutParameter(2, Types.INTEGER);
            call.execute();
            return call.getInt(2);
        }
    }
}
