package dev.canhdominich.cloud.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment", url = "${payment.url}", fallback = PaymentFallback.class)
public interface PaymentClient {

    @GetMapping("/payments/{orderId}")
    String pay(@PathVariable("orderId") long orderId);
}
