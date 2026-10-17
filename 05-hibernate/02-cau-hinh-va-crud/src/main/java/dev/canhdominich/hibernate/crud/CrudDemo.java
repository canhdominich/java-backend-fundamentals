package dev.canhdominich.hibernate.crud;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class CrudDemo {

    private CrudDemo() {
    }

    public static void main(String[] args) {
        try (SessionFactory factory = Configurations.programmatic(DbConfig.fromEnvironment())) {
            long id = create(factory);
            read(factory, id);
            update(factory, id);
            bulkUpdateIsInvisibleToManagedEntities(factory, id);
            delete(factory, id);
            missingRows(factory, id);
        }
    }

    static long create(SessionFactory factory) {
        SqlRecorder.clear();
        long id = UnitOfWork.inTransaction(factory, session -> {
            Student student = new Student("Nguyễn Văn An", "an@example.com");
            session.persist(student);
            return student.getId();
        });
        SqlRecorder.print("-- create: persist inside one transaction");
        return id;
    }

    static void read(SessionFactory factory, long id) {
        UnitOfWork.run(factory, session -> System.out.println("-- read: " + session.find(Student.class, id)));
        SqlRecorder.print("-- SQL of the read");
    }

    static void update(SessionFactory factory, long id) {
        UnitOfWork.run(factory, session -> session.find(Student.class, id).setFullName("Nguyễn Văn An (cập nhật)"));
        SqlRecorder.print("-- update: load, change, commit");
    }

    static void bulkUpdateIsInvisibleToManagedEntities(SessionFactory factory, long id) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Student student = session.find(Student.class, id);
            int rows = session.createMutationQuery("update Student s set s.level = :level where s.id = :id").setParameter("level", Student.Level.ADVANCED).setParameter("id", id).executeUpdate();
            System.out.println("-- HQL bulk update changed " + rows + " row; the managed object still says " + student.getLevel());
            session.refresh(student);
            System.out.println("-- after refresh() the object says " + student.getLevel());
            tx.commit();
        }
        SqlRecorder.print("-- SQL of the bulk update scenario");
    }

    static void delete(SessionFactory factory, long id) {
        UnitOfWork.run(factory, session -> session.remove(session.find(Student.class, id)));
        SqlRecorder.print("-- delete: load then remove");
    }

    static void missingRows(SessionFactory factory, long id) {
        UnitOfWork.run(factory, session -> {
            System.out.println("-- find() of a missing row returns " + session.find(Student.class, id));
            Student reference = session.getReference(Student.class, id);
            try {
                reference.getFullName();
            } catch (RuntimeException e) {
                System.out.println("-- getReference() of a missing row fails only when used: " + e.getClass().getSimpleName());
            }
        });
    }
}
