package dev.canhdominich.shop.order;

import dev.canhdominich.shop.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @Operation(summary = "Place an order; repeat the Idempotency-Key header to retry safely")
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt,
                                               @RequestHeader(value = "Idempotency-Key", required = false) @Size(max = 80) String idempotencyKey,
                                               @Valid @RequestBody OrderRequest request) {
        OrderService.Placed placed = orders.place(userId(jwt), idempotencyKey, request);
        return ResponseEntity.status(placed.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(placed.replayed()))
                .body(placed.order());
    }

    @GetMapping
    @Operation(summary = "List my orders, newest first")
    public PageResponse<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(orders.list(userId(jwt), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one of my orders with its lines; an admin can read any order")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return orders.get(id, userId(jwt), isAdmin(jwt));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order that is still CREATED and return its stock")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return orders.cancel(id, userId(jwt), isAdmin(jwt));
    }

    private static long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }

    private static boolean isAdmin(Jwt jwt) {
        return jwt.getClaimAsStringList("roles").contains("ADMIN");
    }
}
