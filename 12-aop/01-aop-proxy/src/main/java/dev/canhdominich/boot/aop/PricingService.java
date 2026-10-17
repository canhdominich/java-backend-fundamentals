package dev.canhdominich.boot.aop;

import org.springframework.stereotype.Service;

@Service
public class PricingService implements PriceCalculator {

    @Override
    public int priceOf(String item) {
        return item.length() * 1000;
    }
}
