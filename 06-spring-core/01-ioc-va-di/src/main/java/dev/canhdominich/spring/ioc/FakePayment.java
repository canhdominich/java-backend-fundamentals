package dev.canhdominich.spring.ioc;

import java.util.ArrayList;
import java.util.List;

public class FakePayment implements PaymentService {

    private final List<Integer> charges = new ArrayList<>();

    @Override
    public String charge(int amountVnd) {
        charges.add(amountVnd);
        return "fake:" + amountVnd;
    }

    public List<Integer> charges() {
        return charges;
    }
}
