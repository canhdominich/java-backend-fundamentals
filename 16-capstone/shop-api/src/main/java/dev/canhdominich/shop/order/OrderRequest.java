package dev.canhdominich.shop.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(@NotEmpty @Size(max = 50) List<@Valid Line> lines) {

    public record Line(@NotNull Long productId, @Positive @Max(1000) int quantity) {
    }
}
