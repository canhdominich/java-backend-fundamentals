package dev.canhdominich.shop.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 40) String sku,
        @NotBlank @Size(max = 200) String name,
        @PositiveOrZero long priceCents,
        @PositiveOrZero int stock) {
}
