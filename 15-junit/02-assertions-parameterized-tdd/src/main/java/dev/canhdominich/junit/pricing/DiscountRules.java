package dev.canhdominich.junit.pricing;

public interface DiscountRules {

    int percent(int quantity, boolean member);
}
