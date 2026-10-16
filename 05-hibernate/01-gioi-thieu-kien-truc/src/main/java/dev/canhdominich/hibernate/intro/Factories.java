package dev.canhdominich.hibernate.intro;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class Factories {

    private Factories() {
    }

    public static SessionFactory build(DbConfig config, Class<?>... entities) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.connection.url", config.url())
                .setProperty("hibernate.connection.username", config.user())
                .setProperty("hibernate.connection.password", config.password())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.session_factory.statement_inspector", SqlRecorder.class.getName());
        for (Class<?> entity : entities) {
            configuration.addAnnotatedClass(entity);
        }
        return configuration.buildSessionFactory();
    }

    public static SessionFactory student(DbConfig config) {
        return build(config, Student.class);
    }
}
