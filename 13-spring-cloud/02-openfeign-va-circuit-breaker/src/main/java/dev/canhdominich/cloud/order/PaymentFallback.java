package dev.canhdominich.cloud.order;

import org.springframework.stereotype.Component;

@Component
public class PaymentFallback implements PaymentClient {

    @Override
    public String pay(long orderId) {
        return "pending-" + orderId;
    }
}
