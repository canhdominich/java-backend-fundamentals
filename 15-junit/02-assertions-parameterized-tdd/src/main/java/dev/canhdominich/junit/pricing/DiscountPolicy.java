package dev.canhdominich.junit.pricing;

public class DiscountPolicy implements DiscountRules {

    static final int MEMBER_BONUS = 8;
    static final int CAP = 25;

    @Override
    public int percent(int quantity, boolean member) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        int volume = quantity >= 50 ? 20 : quantity >= 10 ? 10 : 0;
        int total = volume + (member ? MEMBER_BONUS : 0);
        return Math.min(total, CAP);
    }
}
