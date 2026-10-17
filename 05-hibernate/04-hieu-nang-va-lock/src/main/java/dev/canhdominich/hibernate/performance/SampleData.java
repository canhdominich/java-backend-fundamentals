package dev.canhdominich.hibernate.performance;

import org.hibernate.SessionFactory;

public final class SampleData {

    public static final int COURSES = 20;
    public static final int TEACHERS = 4;
    public static final int LESSONS_PER_COURSE = 5;
    public static final int REVIEWS_PER_COURSE = 3;

    private SampleData() {
    }

    public static void load(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            Teacher[] teachers = new Teacher[TEACHERS];
            for (int t = 0; t < TEACHERS; t++) {
                teachers[t] = new Teacher("Teacher " + (t + 1));
                session.persist(teachers[t]);
            }
            for (int c = 1; c <= COURSES; c++) {
                Course course = new Course("Course " + c, 30, teachers[c % TEACHERS]);
                for (int l = 1; l <= LESSONS_PER_COURSE; l++) {
                    course.addLesson("Lesson " + c + "." + l);
                }
                for (int r = 1; r <= REVIEWS_PER_COURSE; r++) {
                    course.addReview("Review " + r + " of course " + c);
                }
                session.persist(course);
            }
        });
        SqlRecorder.clear();
        factory.getStatistics().clear();
    }
}
