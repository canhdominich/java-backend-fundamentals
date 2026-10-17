package dev.canhdominich.spring.di;

public class PaypalPayment implements PaymentService {

    @Override
    public String name() {
        return "paypal";
    }
}
