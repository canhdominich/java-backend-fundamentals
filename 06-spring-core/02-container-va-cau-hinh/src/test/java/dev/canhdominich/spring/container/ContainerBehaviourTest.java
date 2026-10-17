package dev.canhdominich.spring.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContainerBehaviourTest {

    @Test
    void aBeanFactoryCreatesBeansOnlyWhenAskedAndNeedsAutowiringSwitchedOn() {
        assertThat(LazyDemo.beanFactoryWithoutAutowiring()).contains("No default constructor found");
        assertThat(LazyDemo.beanFactory()).containsExactly("constructing Engine", "constructing Car");
    }

    @Test
    void anApplicationContextCreatesSingletonsWhileItStarts() {
        assertThat(LazyDemo.applicationContext()).containsExactly("constructing Engine", "constructing Car");
    }

    @Test
    void threeConfigurationStylesGiveTheSameKindOfBean() {
        List<String> lines = ConfigStylesDemo.run();
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("class=ScannedGreeter").contains("scope=singleton").contains("hello from a scanned component");
        assertThat(lines.get(1)).contains("class=Greeter").contains("hello from a @Bean method");
        assertThat(lines.get(2)).contains("class=Greeter").contains("hello from an XML bean");
    }

    @Test
    void everyStereotypeIsAComponentAndGetsTheSameKindOfName() {
        assertThat(StereotypeDemo.run()).containsExactly("courseController", "courseRepository", "courseService", "plainComponent");
    }

    @Test
    void configurationMistakesSurfaceWhenTheContextStartsNotWhenTheBeanIsUsed() {
        assertThat(FailuresDemo.missingBean()).startsWith("failed at startup: NoSuchBeanDefinitionException").contains("type 'dev.canhdominich.spring.container.Engine'");
        assertThat(FailuresDemo.circularConstructor()).startsWith("failed at startup: BeanCurrentlyInCreationException").contains("Requested bean is currently in creation");
        assertThat(FailuresDemo.circularFields()).startsWith("started");
    }
}
