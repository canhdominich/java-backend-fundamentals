package dev.canhdominich.hibernate.mapping;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class Factories {

    private Factories() {
    }

    public static SessionFactory build(DbConfig config) {
        return build(config, true);
    }

    public static SessionFactory build(DbConfig config, boolean snakeCase) {
        Configuration configuration = new Configuration();
        if (snakeCase) {
            configuration.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        }
        return configuration
                .setProperty("hibernate.connection.url", config.url())
                .setProperty("hibernate.connection.username", config.user())
                .setProperty("hibernate.connection.password", config.password())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.session_factory.statement_inspector", SqlRecorder.class.getName())
                .addAnnotatedClass(Student.class)
                .addAnnotatedClass(Classroom.class)
                .addAnnotatedClass(Course.class)
                .addAnnotatedClass(Enrollment.class)
                .addAnnotatedClass(Grade.class)
                .buildSessionFactory();
    }
}
