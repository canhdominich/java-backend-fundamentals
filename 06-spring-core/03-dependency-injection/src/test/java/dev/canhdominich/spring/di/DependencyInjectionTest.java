package dev.canhdominich.spring.di;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyInjectionTest {

    @Test
    void allThreeStylesWorkInsideTheContainerButOnlyConstructorInjectionCannotBeSkippedOutside() {
        String[] results = InjectionStylesDemo.run();
        assertThat(results[0]).isEqualTo("paypal");
        assertThat(results[1]).isEqualTo("paypal");
        assertThat(results[2]).isEqualTo("paypal");
        assertThat(results[3]).isEqualTo("NullPointerException");
        assertThat(results[4]).isEqualTo("NullPointerException");
    }

    @Test
    void twoCandidatesNeedAQualifierOrAPrimary() {
        assertThat(ResolutionDemo.ambiguous()).startsWith("NoUniqueBeanDefinitionException").contains("found 2: paypal,stripe");
        assertThat(ResolutionDemo.withQualifier()).isEqualTo("stripe");
        assertThat(ResolutionDemo.withPrimary()).isEqualTo("paypal");
    }

    @Test
    void aQualifierWinsOverAPrimary() {
        assertThat(ResolutionDemo.qualifierBeatsPrimary()).isEqualTo("stripe");
    }

    @Test
    void optionalDependenciesAreEmptyWhenNoBeanExists() {
        assertThat(OptionalDemo.run()).containsExactly("none", "none", "none");
    }

    @Test
    void collectionsInjectEveryBeanInOrderAndKeyedByName() {
        assertThat(CollectionDemo.run()).containsExactly(List.of("stripe", "paypal"), List.of("paypal", "stripe"));
    }

    @Test
    void valuePlaceholdersAreLenientUntilAStrictConfigurerIsRegistered() {
        assertThat(ValueDemo.run()).isEqualTo("name=Course Platform, owner=Carnell, maxSize=100");
        assertThat(ValueDemo.missing()).isEqualTo("${app.missing}");
        assertThat(ValueDemo.strict()).startsWith("PlaceholderResolutionException: Could not resolve placeholder 'app.missing'");
    }
}
