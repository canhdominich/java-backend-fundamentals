package dev.canhdominich.shop.product;

import dev.canhdominich.shop.error.ConflictException;
import dev.canhdominich.shop.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, Pageable pageable) {
        return products.findByActiveTrueAndNameContainingIgnoreCase(query, pageable).map(ProductResponse::of);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(long id) {
        return ProductResponse.of(products.findByIdAndActiveTrue(id).orElseThrow(() -> new NotFoundException("product " + id + " not found")));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw new ConflictException("DUPLICATE_SKU", "a product with sku " + request.sku() + " already exists");
        }
        return ProductResponse.of(products.save(new Product(request.sku(), request.name(), request.priceCents(), request.stock())));
    }

    @Transactional
    public ProductResponse update(long id, ProductUpdateRequest request) {
        Product product = products.findByIdAndActiveTrue(id).orElseThrow(() -> new NotFoundException("product " + id + " not found"));
        product.update(request.name(), request.priceCents(), request.stock());
        return ProductResponse.of(products.saveAndFlush(product));
    }

    @Transactional
    public void deactivate(long id) {
        Product product = products.findByIdAndActiveTrue(id).orElseThrow(() -> new NotFoundException("product " + id + " not found"));
        product.deactivate();
    }
}
