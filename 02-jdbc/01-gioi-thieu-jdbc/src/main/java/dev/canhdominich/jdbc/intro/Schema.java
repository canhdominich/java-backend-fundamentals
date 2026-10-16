package dev.canhdominich.jdbc.intro;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class Schema {

    private Schema() {
    }

    static void reset(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS product");
            statement.execute("""
                    CREATE TABLE product (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        sku VARCHAR(32) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        stock INT NULL
                    )""");
            statement.execute("INSERT INTO product(sku, name, price, stock) VALUES ('KB-01', 'Keyboard', 49.90, 12), ('MS-02', 'Mouse', 19.50, NULL)");
        }
    }
}
