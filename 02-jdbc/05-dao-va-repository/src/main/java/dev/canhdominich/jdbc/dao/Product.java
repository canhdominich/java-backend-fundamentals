package dev.canhdominich.jdbc.dao;

import java.math.BigDecimal;

public record Product(Long id, String sku, String name, BigDecimal price, int stock) {
}
