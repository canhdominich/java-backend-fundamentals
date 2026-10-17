package dev.canhdominich.hibernate.performance;

import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;

public class NPlusOneDemo {

    public static void main(String[] args) {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) {
        try (SessionFactory factory = Factories.build(config)) {
            SampleData.load(factory);
            lazyCollectionInLoop(factory);
            joinFetch(factory);
            batchSize(factory);
            entityGraph(factory);
            dtoProjection(factory);
            teacherInLoop(factory);
            teacherJoinFetch(factory);
            paginationWithCollectionFetch(factory);
            paginationInTwoSteps(factory);
        }
    }

    static int lazyCollectionInLoop(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Course> courses = session.createQuery("from Course", Course.class).getResultList();
            int total = 0;
            for (Course course : courses) {
                total += course.getLessons().size();
            }
            System.out.println("lessons counted: " + total);
        });
        return SqlRecorder.printAndCount("A. lazy collection in a loop");
    }

    static int joinFetch(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Course> courses = session
                    .createQuery("select distinct c from Course c join fetch c.lessons", Course.class)
                    .getResultList();
            int total = 0;
            for (Course course : courses) {
                total += course.getLessons().size();
            }
            System.out.println("lessons counted: " + total);
        });
        return SqlRecorder.printAndCount("B. join fetch");
    }

    static int batchSize(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Course> courses = session.createQuery("from Course", Course.class).getResultList();
            int total = 0;
            for (Course course : courses) {
                total += course.getReviews().size();
            }
            System.out.println("reviews counted: " + total);
        });
        return SqlRecorder.printAndCount("C. @BatchSize(size = 10) on reviews");
    }

    static int entityGraph(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            RootGraph<Course> graph = session.createEntityGraph(Course.class);
            graph.addAttributeNodes("lessons");
            List<Course> courses = session.createQuery("from Course", Course.class)
                    .setEntityGraph(graph, GraphSemantic.FETCH)
                    .getResultList();
            int total = 0;
            for (Course course : courses) {
                total += course.getLessons().size();
            }
            System.out.println("lessons counted: " + total);
        });
        return SqlRecorder.printAndCount("D. entity graph");
    }

    static int dtoProjection(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<CourseSummary> summaries = session.createQuery(
                    "select new dev.canhdominich.hibernate.performance.CourseSummary(c.title, t.name, count(l)) "
                            + "from Course c join c.teacher t left join c.lessons l "
                            + "group by c.id, c.title, t.name order by c.id",
                    CourseSummary.class).getResultList();
            System.out.println("first summary: " + summaries.get(0));
        });
        return SqlRecorder.printAndCount("E. DTO projection");
    }

    static int teacherInLoop(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Course> courses = session.createQuery("from Course", Course.class).getResultList();
            for (Course course : courses) {
                course.getTeacher().getName();
            }
        });
        return SqlRecorder.printAndCount("F. lazy many-to-one in a loop");
    }

    static int teacherJoinFetch(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Course> courses = session
                    .createQuery("from Course c join fetch c.teacher", Course.class)
                    .getResultList();
            for (Course course : courses) {
                course.getTeacher().getName();
            }
        });
        return SqlRecorder.printAndCount("G. join fetch on many-to-one");
    }

    static int paginationWithCollectionFetch(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Course> page = session
                    .createQuery("select distinct c from Course c join fetch c.lessons order by c.id", Course.class)
                    .setMaxResults(5)
                    .getResultList();
            System.out.println("courses in page: " + page.size());
        });
        return SqlRecorder.printAndCount("H. pagination with a collection fetch");
    }

    static int paginationInTwoSteps(SessionFactory factory) {
        UnitOfWork.run(factory, session -> {
            List<Long> ids = session.createQuery("select c.id from Course c order by c.id", Long.class)
                    .setMaxResults(5)
                    .getResultList();
            List<Course> page = session
                    .createQuery("select distinct c from Course c join fetch c.lessons where c.id in :ids order by c.id", Course.class)
                    .setParameter("ids", ids)
                    .getResultList();
            System.out.println("courses in page: " + page.size());
        });
        return SqlRecorder.printAndCount("I. pagination in two steps");
    }
}
