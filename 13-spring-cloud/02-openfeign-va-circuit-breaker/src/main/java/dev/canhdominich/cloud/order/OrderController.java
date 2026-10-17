package dev.canhdominich.cloud.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final PaymentClient payments;

    public OrderController(PaymentClient payments) {
        this.payments = payments;
    }

    @GetMapping("/orders/{id}/pay")
    public String pay(@PathVariable long id) {
        return payments.pay(id);
    }

    @GetMapping("/orders/{id}/client")
    public String clientType() {
        return payments.getClass().getName();
    }
}
