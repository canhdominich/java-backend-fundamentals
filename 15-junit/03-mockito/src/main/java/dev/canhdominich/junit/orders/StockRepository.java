package dev.canhdominich.junit.orders;

import java.util.Optional;

public interface StockRepository {

    int available(String sku);

    void reserve(String sku, int quantity);

    Optional<String> shelf(String sku);
}
