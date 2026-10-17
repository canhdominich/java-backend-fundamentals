package dev.canhdominich.hibernate.advanced;

import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class Factories {

    private Factories() {
    }

    public static SessionFactory build(DbConfig config, Class<?>... entities) {
        return build(config, Map.of(), entities);
    }

    public static SessionFactory buildWithCache(DbConfig config, Class<?>... entities) {
        return build(config, Map.of(
                "hibernate.cache.use_second_level_cache", "true",
                "hibernate.cache.use_query_cache", "true",
                "hibernate.cache.region.factory_class", "jcache",
                "hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider",
                "hibernate.javax.cache.missing_cache_strategy", "create"), entities);
    }

    public static SessionFactory build(DbConfig config, Map<String, String> extra, Class<?>... entities) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .setProperty("hibernate.connection.url", config.url())
                .setProperty("hibernate.connection.username", config.user())
                .setProperty("hibernate.connection.password", config.password())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.session_factory.statement_inspector", SqlRecorder.class.getName());
        extra.forEach(configuration::setProperty);
        for (Class<?> entity : entities) {
            configuration.addAnnotatedClass(entity);
        }
        return configuration.buildSessionFactory();
    }
}
