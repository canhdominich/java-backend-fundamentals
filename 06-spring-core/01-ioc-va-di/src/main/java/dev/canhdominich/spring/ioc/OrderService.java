package dev.canhdominich.spring.ioc;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final PaymentService payment;

    public OrderService(PaymentService payment) {
        this.payment = payment;
        Events.record("constructing OrderService with " + payment.getClass().getSimpleName());
    }

    public String place(int amountVnd) {
        return payment.charge(amountVnd);
    }
}
