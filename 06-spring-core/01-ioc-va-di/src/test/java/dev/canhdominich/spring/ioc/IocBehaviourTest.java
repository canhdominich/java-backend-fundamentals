package dev.canhdominich.spring.ioc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IocBehaviourTest {

    @Test
    void aHardWiredServiceAlwaysCallsTheRealPayment() {
        assertThat(IocDemo.withoutContainer()).isEqualTo("paypal:100");
        assertThat(Events.all()).containsExactly("constructing PaypalPayment", "PAYPAL charged 100");
    }

    @Test
    void theContainerCreatesTheDependencyBeforeTheBeanThatNeedsIt() {
        List<String> events = IocDemo.withContainer();
        assertThat(events).containsExactly(
                "constructing PaypalPayment",
                "constructing OrderService with PaypalPayment",
                "PAYPAL charged 100");
    }

    @Test
    void aFakeGoesThroughTheConstructorWithoutAnyContainer() {
        assertThat(IocDemo.withFake()).containsExactly(100);
    }

    @Test
    void springCoreIsFiveModules() {
        assertThat(ModulesDemo.run()).containsExactly(
                "spring-aop-7.0.9.jar",
                "spring-beans-7.0.9.jar",
                "spring-context-7.0.9.jar",
                "spring-core-7.0.9.jar",
                "spring-expression-7.0.9.jar");
    }
}
