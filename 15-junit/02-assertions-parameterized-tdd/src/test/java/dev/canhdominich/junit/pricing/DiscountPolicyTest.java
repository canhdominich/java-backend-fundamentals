package dev.canhdominich.junit.pricing;

class DiscountPolicyTest extends DiscountContract {

    @Override
    DiscountRules rules() {
        return new DiscountPolicy();
    }
}
