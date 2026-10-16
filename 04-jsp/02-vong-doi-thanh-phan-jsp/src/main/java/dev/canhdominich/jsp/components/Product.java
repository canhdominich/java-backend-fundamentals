package dev.canhdominich.jsp.components;

import java.math.BigDecimal;

public record Product(String sku, String name, BigDecimal price, Integer stock) {
}
