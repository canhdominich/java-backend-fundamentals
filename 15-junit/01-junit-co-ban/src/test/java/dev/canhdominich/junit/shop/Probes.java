package dev.canhdominich.junit.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

public final class Probes {

    static final List<String> LOG = new ArrayList<>();
    private static final Map<Object, String> NAMES = new IdentityHashMap<>();

    private Probes() {
    }

    static String name(Object instance) {
        return NAMES.computeIfAbsent(instance, key -> "instance " + (char) ('A' + NAMES.size()));
    }

    static void reset() {
        LOG.clear();
        NAMES.clear();
    }

    public static class PerMethod {

        public PerMethod() {
            LOG.add("constructor " + name(this));
        }

        @BeforeAll
        static void beforeAll() {
            LOG.add("beforeAll");
        }

        @BeforeEach
        void beforeEach() {
            LOG.add("beforeEach on " + name(this));
        }

        @Test
        void first() {
            LOG.add("test first on " + name(this));
        }

        @Test
        void second() {
            LOG.add("test second on " + name(this));
        }

        @AfterEach
        void afterEach() {
            LOG.add("afterEach on " + name(this));
        }

        @AfterAll
        static void afterAll() {
            LOG.add("afterAll");
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public static class PerClass {

        public PerClass() {
            LOG.add("constructor " + name(this));
        }

        @BeforeAll
        void beforeAll() {
            LOG.add("beforeAll on " + name(this));
        }

        @Test
        void first() {
            LOG.add("test first on " + name(this));
        }

        @Test
        void second() {
            LOG.add("test second on " + name(this));
        }
    }

    public static class Outcomes {

        @Test
        void passes() {
            assertEquals(4, 2 + 2);
        }

        @Test
        void failsAnAssertion() {
            assertEquals(5, 2 + 2);
        }

        @Test
        void throwsAnException() {
            String missing = null;
            missing.length();
        }

        @Test
        @Disabled("not ready")
        void isDisabled() {
        }

        @Test
        void assumptionIsNotMet() {
            assumeTrue(false);
        }

        @Test
        @Timeout(1)
        void takesTooLong() throws InterruptedException {
            Thread.sleep(2000);
        }
    }

    public static class DefaultOrder {

        @Test
        void zulu() {
            LOG.add("zulu");
        }

        @Test
        void alpha() {
            LOG.add("alpha");
        }

        @Test
        void mike() {
            LOG.add("mike");
        }

        @Test
        void bravo() {
            LOG.add("bravo");
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class ExplicitOrder {

        @Test
        @Order(3)
        void zulu() {
            LOG.add("zulu");
        }

        @Test
        @Order(1)
        void alpha() {
            LOG.add("alpha");
        }

        @Test
        @Order(2)
        void mike() {
            LOG.add("mike");
        }
    }

    public static class Tagged {

        @Test
        @Tag("smoke")
        void smokeOne() {
        }

        @Test
        @Tag("smoke")
        void smokeTwo() {
        }

        @Test
        @Tag("slow")
        void slowOne() {
        }

        @Test
        void untagged() {
        }
    }

    public static class Repeated {

        @RepeatedTest(3)
        void again() {
            LOG.add("repetition ran");
        }
    }
}
