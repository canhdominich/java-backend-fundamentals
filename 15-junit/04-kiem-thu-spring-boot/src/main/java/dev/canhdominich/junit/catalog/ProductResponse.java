package dev.canhdominich.junit.catalog;

public record ProductResponse(Long id, String name, long price) {

    static ProductResponse of(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }
}
