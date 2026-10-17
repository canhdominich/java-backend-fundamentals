package dev.canhdominich.junit.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProductRequest(@NotBlank String name, @Positive long price) {
}
