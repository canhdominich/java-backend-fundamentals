package dev.canhdominich.junit.catalog;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("product " + id + " not found");
    }
}
