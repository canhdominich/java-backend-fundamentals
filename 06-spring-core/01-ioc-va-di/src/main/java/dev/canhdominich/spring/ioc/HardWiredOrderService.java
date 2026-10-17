package dev.canhdominich.spring.ioc;

public class HardWiredOrderService {

    private final PaymentService payment = new PaypalPayment();

    public String place(int amountVnd) {
        return payment.charge(amountVnd);
    }
}
