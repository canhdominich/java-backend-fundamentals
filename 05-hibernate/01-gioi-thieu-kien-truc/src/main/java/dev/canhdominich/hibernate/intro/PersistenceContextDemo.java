package dev.canhdominich.hibernate.intro;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class PersistenceContextDemo {

    private PersistenceContextDemo() {
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        try (SessionFactory factory = Factories.student(DbConfig.fromEnvironment())) {
            long factoryMs = (System.nanoTime() - start) / 1_000_000;
            start = System.nanoTime();
            try (Session session = factory.openSession()) {
                System.out.println("-- building the SessionFactory took more than 20 times longer than opening a Session: " + (factoryMs > 20 * ((System.nanoTime() - start) / 1_000_000 + 1)));
            }

            long id = persistAndCommit(factory);
            firstLevelCache(factory, id);
            dirtyChecking(factory, id);
            entityStates(factory, id);
            autoFlushBeforeQuery(factory, id);
            proxies(factory, id);
        }
    }

    static long persistAndCommit(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = new Student("Nguyễn Văn An", "an@example.com");
            System.out.println("-- persist: state before = transient, id = " + student.getId());
            session.persist(student);
            SqlRecorder.print("-- SQL right after persist(), before commit (IDENTITY must know the id)");
            System.out.println("-- state after persist: managed = " + session.contains(student) + ", id = " + student.getId());
            tx.commit();
            SqlRecorder.print("-- SQL at commit");
            return student.getId();
        }
    }

    static void firstLevelCache(SessionFactory factory, long id) {
        try (Session session = factory.openSession()) {
            Student first = session.find(Student.class, id);
            Student second = session.find(Student.class, id);
            System.out.println("-- two find() calls in one Session: same object = " + (first == second));
            SqlRecorder.print("-- SQL for two find() calls");
            session.clear();
            session.find(Student.class, id);
            SqlRecorder.print("-- SQL for a find() after clear()");
        }
    }

    static void dirtyChecking(SessionFactory factory, long id) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            SqlRecorder.clear();
            student.setFullName("Nguyễn Văn An (đã sửa)");
            SqlRecorder.print("-- SQL after setFullName(), before commit");
            tx.commit();
            SqlRecorder.print("-- SQL at commit (no update() call was made)");
        }
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            SqlRecorder.clear();
            student.setFullName(student.getFullName());
            tx.commit();
            SqlRecorder.print("-- SQL when the value is set to what it already was");
        }
    }

    static void entityStates(SessionFactory factory, long id) {
        Student detached;
        try (Session session = factory.openSession()) {
            detached = session.find(Student.class, id);
        }
        System.out.println("-- after the Session closed: still usable as an object = " + detached.getFullName());
        detached.setEmail("changed-while-detached@example.com");
        SqlRecorder.clear();
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student merged = session.merge(detached);
            System.out.println("-- merge() returns a different object: " + (merged != detached) + ", the argument is managed: " + session.contains(detached) + ", the result is managed: " + session.contains(merged));
            tx.commit();
            SqlRecorder.print("-- SQL for merge() of a detached object");
        }
    }

    static void autoFlushBeforeQuery(SessionFactory factory, long id) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            SqlRecorder.clear();
            student.setLevel(Student.Level.ADVANCED);
            Long advanced = session.createQuery("select count(s) from Student s where s.level = :level", Long.class).setParameter("level", Student.Level.ADVANCED).getSingleResult();
            System.out.println("-- a query after an in-memory change counts the change: " + advanced);
            SqlRecorder.print("-- SQL: the pending UPDATE is flushed before the query");
            tx.rollback();
        }
    }

    static void proxies(SessionFactory factory, long id) {
        try (Session session = factory.openSession()) {
            SqlRecorder.clear();
            Student reference = session.getReference(Student.class, id);
            System.out.println("-- getReference(): initialized = " + Hibernate.isInitialized(reference) + ", class = " + reference.getClass().getSimpleName().replaceAll("\\$.*", "") + " subclass");
            SqlRecorder.print("-- SQL after getReference()");
            System.out.println("-- reading the id does not load it, reading a field does: id = " + reference.getId());
            SqlRecorder.print("-- SQL after getId()");
            System.out.println("-- name = " + reference.getFullName());
            SqlRecorder.print("-- SQL after getFullName()");
        }
    }
}
