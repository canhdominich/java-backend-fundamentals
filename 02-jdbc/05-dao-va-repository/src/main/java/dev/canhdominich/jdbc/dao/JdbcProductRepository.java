package dev.canhdominich.jdbc.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcProductRepository implements ProductRepository {

    private static final String COLUMNS = "id, sku, name, price, stock";
    private final DataSource dataSource;

    public JdbcProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Product insert(String sku, String name, java.math.BigDecimal price, int stock) {
        return TransactionContext.withConnection(dataSource, "insert product " + sku, connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO product(sku, name, price, stock) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sku);
                ps.setString(2, name);
                ps.setBigDecimal(3, price);
                ps.setInt(4, stock);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Product(keys.getLong(1), sku, name, price, stock);
                }
            }
        });
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return TransactionContext.withConnection(dataSource, "find product " + sku, connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT " + COLUMNS + " FROM product WHERE sku = ?")) {
                ps.setString(1, sku);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Product> findAll() {
        return query("SELECT " + COLUMNS + " FROM product ORDER BY id", "find all products");
    }

    @Override
    public List<Product> findPageByOffset(int offset, int size) {
        return TransactionContext.withConnection(dataSource, "page by offset", connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT " + COLUMNS + " FROM product ORDER BY id LIMIT ? OFFSET ?")) {
                ps.setInt(1, size);
                ps.setInt(2, offset);
                return read(ps);
            }
        });
    }

    @Override
    public List<Product> findPageAfter(long afterId, int size) {
        return TransactionContext.withConnection(dataSource, "page after id", connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT " + COLUMNS + " FROM product WHERE id > ? ORDER BY id LIMIT ?")) {
                ps.setLong(1, afterId);
                ps.setInt(2, size);
                return read(ps);
            }
        });
    }

    @Override
    public boolean decreaseStock(long productId, int quantity) {
        return TransactionContext.withConnection(dataSource, "decrease stock", connection -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
                ps.setInt(1, quantity);
                ps.setLong(2, productId);
                ps.setInt(3, quantity);
                return ps.executeUpdate() == 1;
            }
        });
    }

    private List<Product> query(String sql, String description) {
        return TransactionContext.withConnection(dataSource, description, connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                return read(ps);
            }
        });
    }

    private static List<Product> read(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Product> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    private static Product map(ResultSet rs) throws SQLException {
        return new Product(rs.getLong("id"), rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("stock"));
    }
}
