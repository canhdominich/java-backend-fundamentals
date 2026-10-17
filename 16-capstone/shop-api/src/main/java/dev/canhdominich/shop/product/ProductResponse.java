package dev.canhdominich.shop.product;

public record ProductResponse(Long id, String sku, String name, long priceCents, int stock, boolean active, long version) {

    static ProductResponse of(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getPriceCents(), product.getStock(), product.isActive(), product.getVersion());
    }
}
