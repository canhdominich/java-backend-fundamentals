package dev.canhdominich.jdbc.crud;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Product(Long id, String sku, String name, BigDecimal price, int stock, LocalDateTime createdAt) {
}
