package dev.canhdominich.junit.pricing;

public final class Stages {

    private Stages() {
    }

    public static class Red extends DiscountContract {

        @Override
        DiscountRules rules() {
            return (quantity, member) -> 0;
        }
    }

    public static class FirstGreen extends DiscountContract {

        @Override
        DiscountRules rules() {
            return (quantity, member) -> quantity >= 10 ? 10 : 0;
        }
    }

    public static class MoreRules extends DiscountContract {

        @Override
        DiscountRules rules() {
            return (quantity, member) -> {
                if (quantity < 1) {
                    throw new IllegalArgumentException("quantity must be positive: " + quantity);
                }
                return (quantity >= 50 ? 20 : quantity >= 10 ? 10 : 0) + (member ? 8 : 0);
            };
        }
    }

    public static class Final extends DiscountContract {

        @Override
        DiscountRules rules() {
            return new DiscountPolicy();
        }
    }
}
