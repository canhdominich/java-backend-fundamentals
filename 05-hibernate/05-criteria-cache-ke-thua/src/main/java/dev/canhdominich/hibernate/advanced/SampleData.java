package dev.canhdominich.hibernate.advanced;

import org.hibernate.SessionFactory;

public final class SampleData {

    private SampleData() {
    }

    public static void loadCourses(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            String[] levels = {"BASIC", "INTERMEDIATE", "ADVANCED"};
            for (int i = 1; i <= 30; i++) {
                session.persist(new Course((i % 2 == 0 ? "Java " : "Spring ") + "course " + i, levels[i % 3], 100_000 * (1 + i % 5)));
            }
        });
        SqlRecorder.clear();
        factory.getStatistics().clear();
    }
}
