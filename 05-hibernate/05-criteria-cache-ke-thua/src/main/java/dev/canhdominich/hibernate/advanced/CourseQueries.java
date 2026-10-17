package dev.canhdominich.hibernate.advanced;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;

public final class CourseQueries {

    private CourseQueries() {
    }

    public static List<Course> search(Session session, CourseSearch search, int page, int pageSize) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Course> query = cb.createQuery(Course.class);
        Root<Course> course = query.from(Course.class);
        query.select(course).where(predicates(cb, course, search).toArray(new Predicate[0])).orderBy(cb.asc(course.get("id")));
        return session.createQuery(query).setFirstResult(page * pageSize).setMaxResults(pageSize).getResultList();
    }

    public static long count(Session session, CourseSearch search) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Course> course = query.from(Course.class);
        query.select(cb.count(course)).where(predicates(cb, course, search).toArray(new Predicate[0]));
        return session.createQuery(query).getSingleResult();
    }

    private static List<Predicate> predicates(CriteriaBuilder cb, Root<Course> course, CourseSearch search) {
        List<Predicate> predicates = new ArrayList<>();
        if (search.titleContains() != null) {
            predicates.add(cb.like(course.get("title"), "%" + search.titleContains() + "%"));
        }
        if (search.level() != null) {
            predicates.add(cb.equal(course.get("level"), search.level()));
        }
        if (search.minPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(course.get("priceVnd"), search.minPrice()));
        }
        if (search.maxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(course.get("priceVnd"), search.maxPrice()));
        }
        return predicates;
    }
}
