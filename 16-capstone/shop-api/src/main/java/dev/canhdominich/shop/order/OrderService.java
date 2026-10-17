package dev.canhdominich.shop.order;

import dev.canhdominich.shop.error.NotFoundException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    public record Placed(OrderResponse order, boolean replayed) {
    }

    private final OrderPlacement placement;
    private final OrderRepository orders;

    public OrderService(OrderPlacement placement, OrderRepository orders) {
        this.placement = placement;
        this.orders = orders;
    }

    public Placed place(long userId, String idempotencyKey, OrderRequest request) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orders.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return new Placed(detail(existing.get().getId()), true);
            }
        }
        try {
            Order created = placement.execute(userId, idempotencyKey, request);
            return new Placed(detail(created.getId()), false);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey == null) {
                throw e;
            }
            Order existing = orders.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElseThrow(() -> e);
            return new Placed(detail(existing.getId()), true);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(long userId, Pageable pageable) {
        return orders.findByUserId(userId, pageable).map(OrderResponse::summary);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long orderId, long userId, boolean admin) {
        Order order = orders.findWithLinesById(orderId).orElseThrow(() -> new NotFoundException("order " + orderId + " not found"));
        if (!admin && !order.getUser().getId().equals(userId)) {
            throw new NotFoundException("order " + orderId + " not found");
        }
        return OrderResponse.detail(order);
    }

    public OrderResponse cancel(long orderId, long userId, boolean admin) {
        get(orderId, userId, admin);
        placement.cancel(orderId);
        return detail(orderId);
    }

    OrderResponse detail(long orderId) {
        return OrderResponse.detail(orders.findWithLinesById(orderId).orElseThrow());
    }
}
