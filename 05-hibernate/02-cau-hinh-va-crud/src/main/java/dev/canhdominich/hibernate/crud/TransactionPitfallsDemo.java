package dev.canhdominich.hibernate.crud;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class TransactionPitfallsDemo {

    private TransactionPitfallsDemo() {
    }

    public static void main(String[] args) {
        try (SessionFactory factory = Configurations.programmatic(DbConfig.fromEnvironment())) {
            reset(factory);
            detachedObjects(factory);
            reset(factory);
            forgottenCommit(factory);
            constraintViolation(factory);
            rollbackKeepsMemoryState(factory);
        }
    }

    static void reset(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            session.createMutationQuery("delete from Student").executeUpdate();
            session.persist(new Student("An", "an@example.com"));
        });
    }

    static long rows(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            return session.createQuery("select count(s) from Student s", Long.class).getSingleResult();
        }
    }

    static void detachedObjects(SessionFactory factory) {
        Student detached;
        try (Session session = factory.openSession()) {
            detached = session.createQuery("from Student", Student.class).getSingleResult();
        }
        try {
            UnitOfWork.run(factory, session -> session.persist(detached));
        } catch (RuntimeException e) {
            System.out.println("-- persist(detached): " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        try {
            UnitOfWork.run(factory, session -> session.remove(detached));
            System.out.println("-- remove(detached): no exception, rows left = " + rows(factory));
        } catch (RuntimeException e) {
            System.out.println("-- remove(detached): " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static void forgottenCommit(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            session.persist(new Student("Forgotten", "forgotten@example.com"));
        }
        try (Session session = factory.openSession()) {
            Long count = session.createQuery("select count(s) from Student s where s.email = 'forgotten@example.com'", Long.class).getSingleResult();
            System.out.println("-- a Session closed without commit: rows stored = " + count);
        }
    }

    static void constraintViolation(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.persist(new Student("Duplicate", "an@example.com"));
                tx.commit();
            } catch (RuntimeException e) {
                System.out.println("-- duplicate email, raised by persist() because IDENTITY inserts at once: " + e.getClass().getSimpleName() + ", cause " + e.getCause().getClass().getSimpleName());
                System.out.println("-- transaction status after the failure: " + tx.getStatus() + ", active = " + tx.isActive());
            }
        }
    }

    static void rollbackKeepsMemoryState(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.createQuery("from Student s where s.email = 'an@example.com'", Student.class).getSingleResult();
            student.setFullName("Changed then rolled back");
            tx.rollback();
            System.out.println("-- after rollback the object in memory still says: " + student.getFullName());
        }
        try (Session session = factory.openSession()) {
            System.out.println("-- and the database says: " + session.createQuery("from Student s where s.email = 'an@example.com'", Student.class).getSingleResult().getFullName());
        }
    }
}
