package dev.canhdominich.hibernate.advanced;

import java.util.List;
import org.hibernate.SessionFactory;

public class CriteriaDemo {

    public static void main(String[] args) {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) {
        try (SessionFactory factory = Factories.build(config, Course.class)) {
            SampleData.loadCourses(factory);
            search(factory, "no filter", CourseSearch.none());
            search(factory, "level only", new CourseSearch(null, "ADVANCED", null, null));
            search(factory, "title, level and price range", new CourseSearch("Java", "ADVANCED", 200_000, 400_000));
            countForPagination(factory);
            hostileInput(factory);
            unknownAttribute(factory);
        }
    }

    static List<Course> search(SessionFactory factory, String label, CourseSearch search) {
        List<Course> found = UnitOfWork.inTransaction(factory, session -> CourseQueries.search(session, search, 0, 5));
        System.out.println(label + ": " + found.size() + " courses on the first page");
        SqlRecorder.print("SQL of the criteria query");
        return found;
    }

    static long countForPagination(SessionFactory factory) {
        long total = UnitOfWork.inTransaction(factory, session -> CourseQueries.count(session, new CourseSearch(null, "ADVANCED", null, null)));
        System.out.println("total for the same filter: " + total);
        SqlRecorder.print("SQL of the count query");
        return total;
    }

    static int hostileInput(SessionFactory factory) {
        CourseSearch hostile = new CourseSearch("' or 1=1 --", null, null, null);
        int found = UnitOfWork.inTransaction(factory, session -> CourseQueries.search(session, hostile, 0, 5)).size();
        System.out.println("title filter \"' or 1=1 --\": " + found + " courses");
        SqlRecorder.print("SQL (the value travels as a parameter)");
        return found;
    }

    static String unknownAttribute(SessionFactory factory) {
        try {
            UnitOfWork.run(factory, session -> {
                var cb = session.getCriteriaBuilder();
                var query = cb.createQuery(Course.class);
                var root = query.from(Course.class);
                query.select(root).where(cb.equal(root.get("titel"), "x"));
                session.createQuery(query).getResultList();
            });
            return "no error";
        } catch (RuntimeException e) {
            String message = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.out.println("misspelled attribute: " + message);
            return message;
        }
    }
}
