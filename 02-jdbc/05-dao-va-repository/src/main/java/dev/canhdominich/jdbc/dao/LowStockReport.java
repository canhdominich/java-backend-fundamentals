package dev.canhdominich.jdbc.dao;

import java.util.List;

public final class LowStockReport {

    private final ProductRepository products;

    public LowStockReport(ProductRepository products) {
        this.products = products;
    }

    public List<String> namesBelow(int threshold) {
        return products.findAll().stream().filter(product -> product.stock() < threshold).map(Product::name).sorted().toList();
    }
}
