package dev.canhdominich.hibernate.mapping;

import java.util.List;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public final class LazyLoadingDemo {

    private LazyLoadingDemo() {
    }

    public static void main(String[] args) {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) {
        try (SessionFactory factory = Factories.build(config)) {
            SampleData.load(factory, 20);
            lazyInitialisationException(factory);
            nPlusOne(factory);
            joinFetch(factory);
        }
    }

    static void lazyInitialisationException(SessionFactory factory) {
        Student detached;
        try (Session session = factory.openSession()) {
            detached = session.createQuery("from Student s where s.id = 1", Student.class).getSingleResult();
        }
        try {
            detached.getEnrollments().size();
        } catch (LazyInitializationException e) {
            System.out.println("-- collection touched after the Session closed: " + e.getClass().getSimpleName());
        }
        SqlRecorder.clear();
    }

    static void nPlusOne(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            List<Student> students = session.createQuery("from Student s", Student.class).getResultList();
            int enrollments = 0;
            for (Student student : students) {
                enrollments += student.getEnrollments().size();
            }
            System.out.println("-- " + students.size() + " students, " + enrollments + " enrollments counted by touching each collection");
            System.out.println("-- statements sent: " + SqlRecorder.statements().size());
            SqlRecorder.clear();
        }
    }

    static void joinFetch(SessionFactory factory) {
        try (Session session = factory.openSession()) {
            List<Student> students = session.createQuery("select distinct s from Student s join fetch s.enrollments", Student.class).getResultList();
            int enrollments = 0;
            for (Student student : students) {
                enrollments += student.getEnrollments().size();
            }
            System.out.println("-- " + students.size() + " students, " + enrollments + " enrollments with join fetch");
            System.out.println("-- statements sent: " + SqlRecorder.statements().size());
            SqlRecorder.print("-- SQL");
        }
    }
}
