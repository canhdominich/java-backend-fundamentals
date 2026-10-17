package dev.canhdominich.hibernate.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

class AdvancedBehaviourTest {

    @Test
    void criteriaBuildsOnlyTheConditionsThatWereRequested() {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser(), Course.class)) {
            SampleData.loadCourses(factory);
            UnitOfWork.run(factory, session -> CourseQueries.search(session, CourseSearch.none(), 0, 5));
            assertThat(SqlRecorder.statements()).singleElement().asString().doesNotContain("where");
            SqlRecorder.clear();
            UnitOfWork.run(factory, session -> CourseQueries.search(session, new CourseSearch("Java", "ADVANCED", 200_000, 400_000), 0, 5));
            assertThat(SqlRecorder.statements()).singleElement().asString()
                    .contains("title like ? escape ''")
                    .contains("level=?")
                    .contains("price_vnd>=?")
                    .contains("price_vnd<=?");
        }
    }

    @Test
    void criteriaPaginationNeedsACountQueryWithTheSameFilter() {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser(), Course.class)) {
            SampleData.loadCourses(factory);
            assertThat(CriteriaDemo.countForPagination(factory)).isEqualTo(10);
        }
    }

    @Test
    void criteriaValuesTravelAsParametersAndMisspelledAttributesFailAtRuntime() {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser(), Course.class)) {
            SampleData.loadCourses(factory);
            assertThat(CriteriaDemo.hostileInput(factory)).isZero();
            assertThat(CriteriaDemo.unknownAttribute(factory)).contains("Could not resolve attribute 'titel'");
        }
    }

    @Test
    void firstLevelCacheNeedsNoConfigurationAndSecondLevelCacheIsOptIn() {
        try (SessionFactory off = Factories.build(MySqlLab.appUser(), Map.of("hibernate.cache.use_second_level_cache", "false"), Course.class)) {
            SampleData.loadCourses(off);
            assertThat(CacheDemo.sameSessionTwice(off)).isEqualTo(1);
            assertThat(CacheDemo.twoSessions(off, "off")).isEqualTo(2);
        }
        try (SessionFactory on = Factories.buildWithCache(MySqlLab.appUser(), Course.class)) {
            SampleData.loadCourses(on);
            assertThat(CacheDemo.twoSessions(on, "on")).isEqualTo(1);
            assertThat(CacheDemo.queryCache(on)).isEqualTo(1);
        }
    }

    @Test
    void secondLevelCacheServesStaleDataWhenAnotherProgramWritesTheRow() throws Exception {
        try (SessionFactory on = Factories.buildWithCache(MySqlLab.appUser(), Course.class)) {
            SampleData.loadCourses(on);
            assertThat(CacheDemo.updateThroughHibernate(on)).isEqualTo(777_000);
            assertThat(CacheDemo.updateBehindHibernatesBack(on, MySqlLab.appUser())).isEqualTo(500_000);
            assertThat(CacheDemo.evictAndReload(on)).isEqualTo(1);
        }
    }

    @Test
    void eachInheritanceStrategyProducesItsOwnTablesAndQueries() throws Exception {
        List<String> single = InheritanceDemo.singleTable(MySqlLab.appUser());
        assertThat(single).singleElement().asString().contains("card_last4 null").contains("bank_reference null").contains("kind not-null");
        List<String> joined = InheritanceDemo.joined(MySqlLab.appUser());
        assertThat(joined).hasSize(3);
        List<String> perClass = InheritanceDemo.tablePerClass(MySqlLab.appUser());
        assertThat(perClass).hasSize(2);
        assertThat(perClass.get(0)).contains("amount_vnd not-null");
    }

    @Test
    void tablePerClassRejectsIdentityKeys() {
        assertThat(InheritanceDemo.identityWithTablePerClass(MySqlLab.appUser()))
                .contains("Cannot use identity column key generation with <union-subclass> mapping");
    }
}
