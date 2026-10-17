package dev.canhdominich.hibernate.crud;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class Configurations {

    private Configurations() {
    }

    private static Map<String, String> connection(DbConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put("hibernate.connection.url", config.url());
        map.put("hibernate.connection.username", config.user());
        map.put("hibernate.connection.password", config.password());
        return map;
    }

    public static SessionFactory programmatic(DbConfig config) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.session_factory.statement_inspector", SqlRecorder.class.getName())
                .addAnnotatedClass(Student.class);
        connection(config).forEach(configuration::setProperty);
        return configuration.buildSessionFactory();
    }

    public static SessionFactory fromPropertiesFile(DbConfig config) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Configurations.class.getResourceAsStream("/lab/hibernate-lab.properties")) {
            properties.load(in);
        }
        Configuration configuration = new Configuration().addProperties(properties).addAnnotatedClass(Student.class);
        connection(config).forEach(configuration::setProperty);
        return configuration.buildSessionFactory();
    }

    public static SessionFactory fromCfgXml(DbConfig config) {
        Configuration configuration = new Configuration().configure();
        connection(config).forEach(configuration::setProperty);
        return configuration.buildSessionFactory();
    }

    public static EntityManagerFactory fromPersistenceXml(DbConfig config) {
        Map<String, Object> overrides = new HashMap<>(connection(config));
        return Persistence.createEntityManagerFactory("lab", overrides);
    }

    public static SessionFactory withHikari(DbConfig config, int poolSize) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
                .setProperty("hibernate.hikari.maximumPoolSize", String.valueOf(poolSize))
                .setProperty("hibernate.session_factory.statement_inspector", SqlRecorder.class.getName())
                .addAnnotatedClass(Student.class);
        connection(config).forEach(configuration::setProperty);
        return configuration.buildSessionFactory();
    }

    public static SessionFactory withBuiltInPool(DbConfig config) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider")
                .addAnnotatedClass(Student.class);
        connection(config).forEach(configuration::setProperty);
        return configuration.buildSessionFactory();
    }
}
