package dev.canhdominich.junit.orders;

public interface PaymentGateway {

    String charge(String customer, long amount);

    void refund(String transactionId);
}
