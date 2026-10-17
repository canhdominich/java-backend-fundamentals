package dev.canhdominich.hibernate.mapping;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public final class HqlDemo {

    public record CityCount(String city, Long students) {
    }

    private HqlDemo() {
    }

    public static void main(String[] args) {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) {
        try (SessionFactory factory = Factories.build(config)) {
            SampleData.load(factory, 20);
            try (Session session = factory.openSession()) {
                List<Student> advanced = session.createQuery("from Student s where s.level = :level order by s.fullName", Student.class).setParameter("level", Student.Level.ADVANCED).getResultList();
                System.out.println("-- parameter binding: " + advanced.size() + " advanced students");
                SqlRecorder.print("-- SQL of the parameterised query");

                List<Object[]> pairs = session.createQuery("select s.fullName, s.level from Student s where s.id <= 2 order by s.id", Object[].class).getResultList();
                System.out.println("-- projection of two fields returns " + pairs.size() + " Object[] rows, first = " + pairs.get(0)[0] + " / " + pairs.get(0)[1]);
                SqlRecorder.print("-- SQL of the projection (no entity is created)");

                List<CityCount> counts = session.createQuery("select new dev.canhdominich.hibernate.mapping.HqlDemo$CityCount(s.address.city, count(s)) from Student s group by s.address.city order by s.address.city", CityCount.class).getResultList();
                counts.forEach(count -> System.out.println("-- DTO " + count));
                SqlRecorder.print("-- SQL of the aggregate into a record");

                List<Student> page = session.createQuery("from Student s order by s.id", Student.class).setFirstResult(5).setMaxResults(5).getResultList();
                System.out.println("-- page of " + page.size() + " starting at " + page.get(0));
                SqlRecorder.print("-- SQL of the paginated query");

                List<Student> joined = session.createQuery("select s from Student s join s.enrollments e where e.course.title = :title and s.id <= 3", Student.class).setParameter("title", "Hibernate").getResultList();
                System.out.println("-- join through the collection: " + joined.size() + " rows");
                SqlRecorder.print("-- SQL of the join (HQL names classes and fields, SQL names tables and columns)");

                Long total = session.createQuery("select count(s) from Student s", Long.class).getSingleResult();
                System.out.println("-- count = " + total);
                SqlRecorder.clear();
            }
        }
    }
}
