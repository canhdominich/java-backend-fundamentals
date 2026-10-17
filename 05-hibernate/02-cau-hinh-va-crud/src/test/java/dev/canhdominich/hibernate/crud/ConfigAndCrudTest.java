package dev.canhdominich.hibernate.crud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;

class ConfigAndCrudTest {

    private static String provider(SessionFactory factory) {
        try (factory) {
            return ((SessionFactoryImplementor) factory).getServiceRegistry().getService(ConnectionProvider.class).getClass().getSimpleName();
        }
    }

    @Test
    void everyConfigurationStyleEndsUpWithTheSameDialectAndTheHikariProviderChosenByTheClasspath() throws Exception {
        DbConfig config = MySqlLab.appUser();
        assertThat(provider(Configurations.programmatic(config))).isEqualTo("HikariCPConnectionProvider");
        assertThat(provider(Configurations.fromPropertiesFile(config))).isEqualTo("HikariCPConnectionProvider");
        assertThat(provider(Configurations.fromCfgXml(config))).isEqualTo("HikariCPConnectionProvider");
        try (EntityManagerFactory emf = Configurations.fromPersistenceXml(config)) {
            assertThat(provider(emf.unwrap(SessionFactory.class))).isEqualTo("HikariCPConnectionProvider");
        }
        assertThat(provider(Configurations.withBuiltInPool(config))).isEqualTo("DriverManagerConnectionProvider");
        try (SessionFactory factory = Configurations.withHikari(config, 7)) {
            var hikari = ((SessionFactoryImplementor) factory).getServiceRegistry().getService(ConnectionProvider.class).unwrap(com.zaxxer.hikari.HikariDataSource.class);
            assertThat(hikari.getMaximumPoolSize()).isEqualTo(7);
        }
    }

    @Test
    void schemaToolModesBehaveAsDocumented() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            SchemaDemo.run(MySqlLab.appUser());
        } finally {
            System.setOut(original);
        }
        String out = captured.toString();
        assertThat(out).contains("validate       FAILED SchemaManagementException: Schema validation: missing table [student]")
                .contains("validate       FAILED SchemaManagementException: Schema validation: missing column [level] in table [student]")
                .contains("update         started, columns after: [id, full_name, email, legacy_notes, level]")
                .contains("create         started, columns after: [id, email, full_name, level]")
                .contains("create-drop    started, columns after: []");
    }

    @Test
    void crudStepsProduceTheExpectedStatements() {
        try (SessionFactory factory = Configurations.programmatic(MySqlLab.appUser())) {
            SqlRecorder.clear();
            long id = CrudDemo.create(factory);
            assertThat(SqlRecorder.statements()).isEmpty();
            CrudDemo.update(factory, id);
            CrudDemo.delete(factory, id);
            UnitOfWork.run(factory, session -> {
                assertThat(session.find(Student.class, id)).isNull();
                assertThatThrownBy(() -> session.getReference(Student.class, id).getFullName()).isInstanceOf(org.hibernate.ObjectNotFoundException.class);
            });
        }
    }

    @Test
    void bulkHqlUpdateLeavesTheManagedObjectStaleUntilRefresh() {
        try (SessionFactory factory = Configurations.programmatic(MySqlLab.appUser())) {
            long id = UnitOfWork.inTransaction(factory, session -> {
                Student student = new Student("An", "an@example.com");
                session.persist(student);
                return student.getId();
            });
            try (Session session = factory.openSession()) {
                Transaction tx = session.beginTransaction();
                Student student = session.find(Student.class, id);
                session.createMutationQuery("update Student s set s.level = :level where s.id = :id").setParameter("level", Student.Level.ADVANCED).setParameter("id", id).executeUpdate();
                assertThat(student.getLevel()).isEqualTo(Student.Level.BASIC);
                session.refresh(student);
                assertThat(student.getLevel()).isEqualTo(Student.Level.ADVANCED);
                tx.commit();
            }
        }
    }

    @Test
    void transactionPitfalls() {
        try (SessionFactory factory = Configurations.programmatic(MySqlLab.appUser())) {
            TransactionPitfallsDemo.reset(factory);
            Student detached;
            try (Session session = factory.openSession()) {
                detached = session.createQuery("from Student", Student.class).getSingleResult();
            }
            assertThatThrownBy(() -> UnitOfWork.run(factory, session -> session.persist(detached))).isInstanceOf(jakarta.persistence.EntityExistsException.class);
            UnitOfWork.run(factory, session -> session.remove(detached));
            assertThat(TransactionPitfallsDemo.rows(factory)).isZero();

            TransactionPitfallsDemo.reset(factory);
            try (Session session = factory.openSession()) {
                session.beginTransaction();
                session.persist(new Student("Forgotten", "forgotten@example.com"));
            }
            assertThat(TransactionPitfallsDemo.rows(factory)).isEqualTo(1);

            try (Session session = factory.openSession()) {
                Transaction tx = session.beginTransaction();
                assertThatThrownBy(() -> session.persist(new Student("Duplicate", "an@example.com"))).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
                assertThat(tx.getStatus().toString()).isEqualTo("MARKED_ROLLBACK");
            }

            try (Session session = factory.openSession()) {
                Transaction tx = session.beginTransaction();
                Student student = session.createQuery("from Student s where s.email = 'an@example.com'", Student.class).getSingleResult();
                student.setFullName("Changed then rolled back");
                tx.rollback();
                assertThat(student.getFullName()).isEqualTo("Changed then rolled back");
            }
            try (Session session = factory.openSession()) {
                assertThat(session.createQuery("from Student s where s.email = 'an@example.com'", Student.class).getSingleResult().getFullName()).isEqualTo("An");
            }
        }
    }
}
