package dev.canhdominich.boot.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 60) String title,
        @Min(0) int price) {
}
