package dev.canhdominich.jdbc.dao;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product insert(String sku, String name, java.math.BigDecimal price, int stock);

    Optional<Product> findBySku(String sku);

    List<Product> findAll();

    List<Product> findPageByOffset(int offset, int size);

    List<Product> findPageAfter(long afterId, int size);

    boolean decreaseStock(long productId, int quantity);
}
