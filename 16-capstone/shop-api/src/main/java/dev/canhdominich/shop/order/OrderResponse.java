package dev.canhdominich.shop.order;

import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, long totalCents, Instant createdAt, List<Line> lines) {

    public record Line(Long productId, String sku, int quantity, long unitPriceCents) {
    }

    static OrderResponse summary(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalCents(), order.getCreatedAt(), List.of());
    }

    static OrderResponse detail(Order order) {
        List<Line> lines = order.getLines().stream()
                .map(line -> new Line(line.getProduct().getId(), line.getProduct().getSku(), line.getQuantity(), line.getUnitPriceCents()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalCents(), order.getCreatedAt(), lines);
    }
}
