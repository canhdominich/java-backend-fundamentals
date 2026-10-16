package dev.canhdominich.hibernate.intro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HibernateIntroTest {

    private static SessionFactory factory;

    @BeforeAll
    static void start() {
        factory = Factories.build(MySqlLab.appUser(), Student.class, Ticket.class, Note.class);
    }

    @AfterAll
    static void stop() {
        factory.close();
    }

    @BeforeEach
    void reset() {
        SqlRecorder.clear();
    }

    private long newStudent() {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = new Student("An", "an@example.com");
            session.persist(student);
            tx.commit();
            SqlRecorder.clear();
            return student.getId();
        }
    }

    @Test
    void identityInsertRunsAtPersistNotAtCommit() {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = new Student("Binh", "binh@example.com");
            assertThat(student.getId()).isNull();
            session.persist(student);
            assertThat(SqlRecorder.statements()).singleElement().asString().startsWith("insert into student");
            assertThat(student.getId()).isNotNull();
            assertThat(session.contains(student)).isTrue();
            SqlRecorder.clear();
            tx.commit();
            assertThat(SqlRecorder.statements()).isEmpty();
        }
    }

    @Test
    void secondFindInTheSameSessionIsServedFromTheFirstLevelCache() {
        long id = newStudent();
        try (Session session = factory.openSession()) {
            Student first = session.find(Student.class, id);
            Student second = session.find(Student.class, id);
            assertThat(second).isSameAs(first);
            assertThat(SqlRecorder.statements()).hasSize(1);
            session.clear();
            session.find(Student.class, id);
            assertThat(SqlRecorder.statements()).hasSize(2);
        }
    }

    @Test
    void dirtyCheckingUpdatesAtCommitWithAllColumnsAndSkipsUnchangedObjects() {
        long id = newStudent();
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            SqlRecorder.clear();
            student.setFullName("An (edited)");
            assertThat(SqlRecorder.statements()).isEmpty();
            tx.commit();
            assertThat(SqlRecorder.statements()).containsExactly("update student set email=?,full_name=?,level=? where id=?");
        }
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            SqlRecorder.clear();
            student.setFullName(student.getFullName());
            tx.commit();
            assertThat(SqlRecorder.statements()).isEmpty();
        }
    }

    @Test
    void mergeLoadsThenUpdatesAndReturnsADifferentManagedObject() {
        long id = newStudent();
        Student detached;
        try (Session session = factory.openSession()) {
            detached = session.find(Student.class, id);
        }
        detached.setEmail("detached-change@example.com");
        SqlRecorder.clear();
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student merged = session.merge(detached);
            assertThat(merged).isNotSameAs(detached);
            assertThat(session.contains(detached)).isFalse();
            assertThat(session.contains(merged)).isTrue();
            tx.commit();
        }
        List<String> sql = SqlRecorder.statements();
        assertThat(sql).hasSize(2);
        assertThat(sql.get(0)).startsWith("select").endsWith("where s1_0.id=?");
        assertThat(sql.get(1)).startsWith("update student");
    }

    @Test
    void pendingChangesAreFlushedBeforeAQueryRuns() {
        long id = newStudent();
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            SqlRecorder.clear();
            student.setLevel(Student.Level.ADVANCED);
            Long advanced = session.createQuery("select count(s) from Student s where s.level = :level", Long.class).setParameter("level", Student.Level.ADVANCED).getSingleResult();
            assertThat(advanced).isGreaterThanOrEqualTo(1);
            List<String> sql = SqlRecorder.statements();
            assertThat(sql.get(0)).startsWith("update student");
            assertThat(sql.get(1)).startsWith("select count");
            tx.rollback();
        }
    }

    @Test
    void getReferenceReturnsAProxyThatLoadsOnFirstFieldAccess() {
        long id = newStudent();
        try (Session session = factory.openSession()) {
            Student reference = session.getReference(Student.class, id);
            assertThat(Hibernate.isInitialized(reference)).isFalse();
            assertThat(reference.getId()).isEqualTo(id);
            assertThat(SqlRecorder.statements()).isEmpty();
            assertThat(reference.getFullName()).isEqualTo("An");
            assertThat(SqlRecorder.statements()).hasSize(1);
            assertThat(Hibernate.isInitialized(reference)).isTrue();
        }
    }

    @Test
    void sequenceIdsAreAllocatedWithoutInsertsAndDynamicUpdateWritesOnlyChangedColumns() {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(new Ticket("one"));
            session.persist(new Ticket("two"));
            assertThat(SqlRecorder.statements()).isEmpty();
            tx.commit();
            assertThat(SqlRecorder.statements()).hasSize(2).allMatch(sql -> sql.startsWith("insert into Ticket"));
        }
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Note note = new Note("first", "body");
            session.persist(note);
            tx.commit();
            SqlRecorder.clear();
            tx = session.beginTransaction();
            session.find(Note.class, note.getId()).setTitle("second");
            tx.commit();
            assertThat(SqlRecorder.statements()).containsExactly("update Note set title=? where id=?");
        }
    }
}
