package dev.canhdominich.spring.ioc;

import org.springframework.stereotype.Component;

@Component
public class PaypalPayment implements PaymentService {

    public PaypalPayment() {
        Events.record("constructing PaypalPayment");
    }

    @Override
    public String charge(int amountVnd) {
        Events.record("PAYPAL charged " + amountVnd);
        return "paypal:" + amountVnd;
    }
}
