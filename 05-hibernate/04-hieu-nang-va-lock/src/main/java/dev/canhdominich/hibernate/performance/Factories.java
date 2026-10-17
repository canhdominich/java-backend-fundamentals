package dev.canhdominich.hibernate.performance;

import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class Factories {

    private Factories() {
    }

    public static SessionFactory build(DbConfig config) {
        return build(config, Map.of());
    }

    public static SessionFactory build(DbConfig config, Map<String, String> extra) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .setProperty("hibernate.connection.url", config.url())
                .setProperty("hibernate.connection.username", config.user())
                .setProperty("hibernate.connection.password", config.password())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.session_factory.statement_inspector", SqlRecorder.class.getName())
                .addAnnotatedClass(Teacher.class)
                .addAnnotatedClass(Course.class)
                .addAnnotatedClass(Lesson.class)
                .addAnnotatedClass(Review.class)
                .addAnnotatedClass(MetricIdentity.class)
                .addAnnotatedClass(MetricSequence.class);
        extra.forEach(configuration::setProperty);
        return configuration.buildSessionFactory();
    }
}
