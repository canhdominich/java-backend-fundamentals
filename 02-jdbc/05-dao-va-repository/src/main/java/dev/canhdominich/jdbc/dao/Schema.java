package dev.canhdominich.jdbc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

final class Schema {

    private Schema() {
    }

    static void reset(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS order_line");
            statement.execute("DROP TABLE IF EXISTS shop_order");
            statement.execute("DROP TABLE IF EXISTS product");
            statement.execute("""
                    CREATE TABLE product (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        sku VARCHAR(32) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        stock INT NOT NULL
                    ) ENGINE=InnoDB""");
            statement.execute("""
                    CREATE TABLE shop_order (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_email VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB""");
            statement.execute("""
                    CREATE TABLE order_line (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        quantity INT NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES shop_order(id),
                        FOREIGN KEY (product_id) REFERENCES product(id)
                    ) ENGINE=InnoDB""");
        }
    }

    static void seedProducts(DataSource dataSource, int rows) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET SESSION cte_max_recursion_depth = " + (rows + 10));
            statement.execute("INSERT INTO product(sku, name, price, stock) SELECT CONCAT('SKU-', n), CONCAT('Product ', n), 9.99, 100 FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < " + rows + ") SELECT n FROM seq) s");
        }
    }
}
