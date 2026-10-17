package dev.canhdominich.micro.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/orders")
    public String order(@RequestParam String orderId, @RequestParam String sku, @RequestParam int quantity,
                        @RequestParam(defaultValue = "true") boolean safe, @RequestParam(defaultValue = "false") boolean failAfterReserve) {
        return orders.place(orderId, sku, quantity, safe, failAfterReserve);
    }

    @PostMapping("/compensations/retry")
    public String retry() {
        return String.valueOf(orders.retryPendingCompensations());
    }

    @GetMapping("/compensations/pending")
    public String pending() {
        return String.valueOf(orders.pending());
    }

    @GetMapping("/orders/count")
    public String count() {
        return String.valueOf(orders.orderCount());
    }
}
