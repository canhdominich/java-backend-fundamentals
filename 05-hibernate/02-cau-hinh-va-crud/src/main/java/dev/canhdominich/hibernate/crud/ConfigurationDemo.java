package dev.canhdominich.hibernate.crud;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public final class ConfigurationDemo {

    private ConfigurationDemo() {
    }

    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.fromEnvironment();
        describe("programmatic Configuration       ", Configurations.programmatic(config));
        describe("hibernate.properties-style file   ", Configurations.fromPropertiesFile(config));
        describe("hibernate.cfg.xml                 ", Configurations.fromCfgXml(config));
        try (EntityManagerFactory emf = Configurations.fromPersistenceXml(config)) {
            describe("persistence.xml (JPA bootstrap)   ", emf.unwrap(SessionFactory.class));
        }
        describe("hikaricp module, pool size 7       ", Configurations.withHikari(config, 7));
        describe("built-in pool forced explicitly    ", Configurations.withBuiltInPool(config));
    }

    static void describe(String label, SessionFactory factory) {
        try (factory) {
            SessionFactoryImplementor implementor = (SessionFactoryImplementor) factory;
            Dialect dialect = implementor.getJdbcServices().getDialect();
            ConnectionProvider provider = implementor.getServiceRegistry().getService(ConnectionProvider.class);
            String detail = provider.isUnwrappableAs(com.zaxxer.hikari.HikariDataSource.class)
                    ? " maximumPoolSize=" + provider.unwrap(com.zaxxer.hikari.HikariDataSource.class).getMaximumPoolSize() : "";
            System.out.println(label + " dialect=" + dialect.getClass().getSimpleName() + " connectionProvider=" + provider.getClass().getSimpleName() + detail);
        }
    }
}
