package dev.canhdominich.hibernate.mapping;

import java.time.LocalDate;
import org.hibernate.SessionFactory;

public final class SampleData {

    private SampleData() {
    }

    public static void load(SessionFactory factory, int students) {
        UnitOfWork.run(factory, session -> {
            Classroom classroom = new Classroom("Java 01");
            session.persist(classroom);
            Course jdbc = new Course("JDBC");
            Course hibernate = new Course("Hibernate");
            session.persist(jdbc);
            session.persist(hibernate);
            for (int i = 1; i <= students; i++) {
                Student student = new Student("Student " + i, "student" + i + "@example.com");
                student.setBirthDate(LocalDate.of(2000 + i % 10, 1 + i % 12, 1 + i % 28));
                student.setAddress(new Address(i + " Le Loi", i % 2 == 0 ? "Ha Noi" : "Da Nang"));
                if (i % 3 == 0) {
                    student.setLevel(Student.Level.ADVANCED);
                }
                classroom.addStudent(student);
                student.enroll(jdbc, LocalDate.of(2026, 9, 1));
                student.enroll(hibernate, LocalDate.of(2026, 10, 1));
                student.getFavouriteCourses().add(i % 2 == 0 ? jdbc : hibernate);
                session.persist(student);
            }
        });
        SqlRecorder.clear();
    }
}
