package dev.canhdominich.spring.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LifecycleScopeSpelTest {

    @BeforeEach
    void reset() {
        Events.clear();
    }

    @Test
    void callbacksRunInTheDocumentedOrder() {
        assertThat(LifecycleDemo.run()).containsExactly(
                "constructor",
                "dependency injected",
                "BeanNameAware.setBeanName(probe)",
                "BeanPostProcessor before initialization",
                "@PostConstruct",
                "InitializingBean.afterPropertiesSet",
                "initMethod",
                "BeanPostProcessor after initialization",
                "@PreDestroy",
                "DisposableBean.destroy",
                "destroyMethod");
    }

    @Test
    void singletonIsSharedPrototypeIsNewAndPrototypesAreNeverDestroyedByTheContainer() {
        List<String> lines = ScopeDemo.run();
        assertThat(lines.get(0)).matches("singleton ids: (\\d+), \\1");
        assertThat(lines.get(1)).matches("prototype ids: (\\d+), (?!\\1)\\d+");
        assertThat(lines.get(2)).matches("destroyed: \\[destroying singleton #\\d+\\]");
    }

    @Test
    void aPrototypeInjectedIntoASingletonIsFrozenUnlessFetchedPerCall() {
        List<String> lines = TrapDemo.run();
        assertThat(lines.get(0)).matches(".*: (\\d+), \\1, \\1");
        assertThat(lines.get(1)).matches(".*: (\\d+), (?!\\1)\\d+, (?!\\1)\\d+");
        assertThat(lines.get(2)).matches(".*: (\\d+), (?!\\1)\\d+, (?!\\1)\\d+");
    }

    @Test
    void spelEvaluatesLiteralsMethodsNullSafetyAndCollections() {
        assertThat(SpelDemo.expressions()).containsExactly(
                "2 * 60 * 60  ->  7200",
                "'Hello'.length()  ->  5",
                "T(java.lang.Math).max(3, 7)  ->  7",
                "name  ->  Binh",
                "score >= 5 ? 'pass' : 'fail'  ->  fail",
                "nickname ?: 'none'  ->  none",
                "address?.city  ->  null",
                "address.city  ->  SpelEvaluationException EL1007E: Property or field 'city' cannot be found on null",
                "students.?[level == 'ADVANCED'].![name]  ->  [An]",
                "students.![score]  ->  [8, 4]");
    }

    @Test
    void valueMixesPlaceholdersAndExpressions() {
        assertThat(SpelDemo.valueAnnotation()).isEqualTo("hours=2, seconds=7200, cacheSeconds=7200, shout=HI, safe=null, biggest=7");
    }
}
