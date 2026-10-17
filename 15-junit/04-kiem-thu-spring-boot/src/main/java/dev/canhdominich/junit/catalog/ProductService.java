package dev.canhdominich.junit.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public Product find(long id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(String name, long price) {
        return products.save(new Product(name, price));
    }

    public List<Product> byName(String name) {
        return products.findByName(name);
    }
}
