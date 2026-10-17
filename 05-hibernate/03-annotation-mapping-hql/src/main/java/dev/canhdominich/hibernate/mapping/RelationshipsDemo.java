package dev.canhdominich.hibernate.mapping;

import java.time.LocalDate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class RelationshipsDemo {

    private RelationshipsDemo() {
    }

    public static void main(String[] args) {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) {
        try (SessionFactory factory = Factories.build(config)) {
            owningSide(factory);
            cascade(factory);
            orphanRemoval(factory);
            eagerAndLazyDefaults(factory);
        }
    }

    static void owningSide(SessionFactory factory) {
        SqlRecorder.clear();
        long studentId = UnitOfWork.inTransaction(factory, session -> {
            Classroom classroom = new Classroom("Only the inverse side is set");
            session.persist(classroom);
            Student student = new Student("Forgot the owning side", "owning@example.com");
            classroom.getStudents().add(student);
            session.persist(student);
            return student.getId();
        });
        UnitOfWork.run(factory, session -> System.out.println("-- classroom.getStudents().add(student) only: student.classroom in the database = " + session.find(Student.class, studentId).getClassroom()));
        SqlRecorder.clear();
        long fixedId = UnitOfWork.inTransaction(factory, session -> {
            Classroom classroom = new Classroom("Both sides set by addStudent");
            session.persist(classroom);
            Student student = new Student("Synchronised", "sync@example.com");
            classroom.addStudent(student);
            session.persist(student);
            return student.getId();
        });
        UnitOfWork.run(factory, session -> System.out.println("-- classroom.addStudent(student): student.classroom name = " + session.find(Student.class, fixedId).getClassroom().getName()));
        SqlRecorder.clear();
    }

    static void cascade(SessionFactory factory) {
        SqlRecorder.clear();
        long id = UnitOfWork.inTransaction(factory, session -> {
            Course course = new Course("Cascade course");
            session.persist(course);
            Student student = new Student("Cascade", "cascade@example.com");
            student.enroll(course, LocalDate.of(2026, 10, 1));
            student.enroll(course, LocalDate.of(2026, 10, 2));
            session.persist(student);
            return student.getId();
        });
        SqlRecorder.print("-- persist(student) with two new enrollments: CascadeType.ALL persists the children");
        UnitOfWork.run(factory, session -> session.remove(session.find(Student.class, id)));
        SqlRecorder.print("-- remove(student): the children go first");
    }

    static void orphanRemoval(SessionFactory factory) {
        long id = UnitOfWork.inTransaction(factory, session -> {
            Course course = new Course("Orphan course");
            session.persist(course);
            Student student = new Student("Orphan", "orphan@example.com");
            student.enroll(course, LocalDate.of(2026, 10, 1));
            student.enroll(course, LocalDate.of(2026, 10, 2));
            session.persist(student);
            return student.getId();
        });
        SqlRecorder.clear();
        UnitOfWork.run(factory, session -> {
            Student student = session.find(Student.class, id);
            student.getEnrollments().remove(0);
        });
        SqlRecorder.print("-- removing one enrollment from the list (orphanRemoval = true)");
    }

    static void eagerAndLazyDefaults(SessionFactory factory) {
        long enrollmentId = UnitOfWork.inTransaction(factory, session -> {
            Course course = new Course("Fetch course");
            session.persist(course);
            Student student = new Student("Fetch", "fetch@example.com");
            Enrollment enrollment = student.enroll(course, LocalDate.of(2026, 10, 1));
            session.persist(student);
            return enrollment.getId();
        });
        SqlRecorder.clear();
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            Enrollment enrollment = session.find(Enrollment.class, enrollmentId);
            SqlRecorder.print("-- find(Enrollment): student is ManyToOne (EAGER by default), course is declared LAZY");
            enrollment.getCourse().getTitle();
            SqlRecorder.print("-- first access to the lazy course");
            tx.commit();
        }
    }
}
