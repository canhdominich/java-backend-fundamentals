package dev.canhdominich.hibernate.advanced;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

public class CacheDemo {

    public static void main(String[] args) throws SQLException {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) throws SQLException {
        try (SessionFactory off = Factories.build(config, Map.of("hibernate.cache.use_second_level_cache", "false"), Course.class)) {
            SampleData.loadCourses(off);
            sameSessionTwice(off);
            twoSessions(off, "use_second_level_cache=false");
        }
        try (SessionFactory unset = Factories.build(config, Course.class)) {
            SampleData.loadCourses(unset);
            twoSessions(unset, "property not set, jcache and ehcache on the classpath");
        }
        try (SessionFactory on = Factories.buildWithCache(config, Course.class)) {
            SampleData.loadCourses(on);
            twoSessions(on, "everything configured");
            queryCache(on);
            updateThroughHibernate(on);
            updateBehindHibernatesBack(on, config);
            evictAndReload(on);
        }
    }

    static int sameSessionTwice(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            session.find(Course.class, 1L);
            session.find(Course.class, 1L);
        });
        return SqlRecorder.printAndCount("find twice in one Session");
    }

    static int twoSessions(SessionFactory factory, String label) {
        factory.getCache().evictAllRegions();
        factory.getStatistics().clear();
        SqlRecorder.clear();
        UnitOfWork.run(factory, session -> session.find(Course.class, 2L));
        UnitOfWork.run(factory, session -> session.find(Course.class, 2L));
        Statistics stats = factory.getStatistics();
        System.out.println("L2 hits=" + stats.getSecondLevelCacheHitCount() + " misses=" + stats.getSecondLevelCacheMissCount() + " puts=" + stats.getSecondLevelCachePutCount());
        return SqlRecorder.printAndCount("find in two Sessions, " + label);
    }

    static int queryCache(SessionFactory factory) {
        factory.getCache().evictAllRegions();
        factory.getStatistics().clear();
        SqlRecorder.clear();
        for (int i = 0; i < 2; i++) {
            UnitOfWork.run(factory, session -> session
                    .createQuery("from Course c where c.level = :level", Course.class)
                    .setParameter("level", "BASIC")
                    .setCacheable(true)
                    .getResultList());
        }
        Statistics stats = factory.getStatistics();
        System.out.println("query cache hits=" + stats.getQueryCacheHitCount() + " misses=" + stats.getQueryCacheMissCount() + " puts=" + stats.getQueryCachePutCount());
        return SqlRecorder.printAndCount("cacheable query run in two Sessions");
    }

    static int updateThroughHibernate(SessionFactory factory) {
        factory.getCache().evictAllRegions();
        UnitOfWork.run(factory, session -> session.find(Course.class, 3L));
        UnitOfWork.run(factory, session -> session.find(Course.class, 3L).setPriceVnd(777_000));
        SqlRecorder.clear();
        int price = UnitOfWork.inTransaction(factory, session -> session.find(Course.class, 3L).getPriceVnd());
        System.out.println("price read after a change made through Hibernate: " + price);
        SqlRecorder.printAndCount("read after the change");
        return price;
    }

    static int updateBehindHibernatesBack(SessionFactory factory, DbConfig config) throws SQLException {
        factory.getCache().evictAllRegions();
        UnitOfWork.run(factory, session -> session.find(Course.class, 4L));
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("update course set price_vnd = 1 where id = 4");
        }
        SqlRecorder.clear();
        int price = UnitOfWork.inTransaction(factory, session -> session.find(Course.class, 4L).getPriceVnd());
        System.out.println("price read after another program wrote 1 to the row: " + price);
        SqlRecorder.printAndCount("read after the outside change");
        return price;
    }

    static int evictAndReload(SessionFactory factory) {
        factory.getCache().evictEntityData(Course.class);
        SqlRecorder.clear();
        int price = UnitOfWork.inTransaction(factory, session -> session.find(Course.class, 4L).getPriceVnd());
        System.out.println("price read after evictEntityData: " + price);
        SqlRecorder.printAndCount("read after eviction");
        return price;
    }
}
