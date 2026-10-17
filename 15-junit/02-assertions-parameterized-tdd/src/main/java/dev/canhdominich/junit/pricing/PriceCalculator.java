package dev.canhdominich.junit.pricing;

public class PriceCalculator {

    private final DiscountRules rules;

    public PriceCalculator(DiscountRules rules) {
        this.rules = rules;
    }

    public long total(long unitPrice, int quantity, boolean member) {
        int percent = rules.percent(quantity, member);
        return unitPrice * quantity * (100 - percent) / 100;
    }
}
