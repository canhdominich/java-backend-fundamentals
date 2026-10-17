package dev.canhdominich.spring.di;

public class StripePayment implements PaymentService {

    @Override
    public String name() {
        return "stripe";
    }
}
