package dev.canhdominich.boot.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class CourseStore {

    private final Map<Long, Course> courses = new ConcurrentSkipListMap<>();
    private final AtomicLong ids = new AtomicLong();

    public Course create(CourseRequest request) {
        Course course = new Course(ids.incrementAndGet(), request.title(), request.price());
        courses.put(course.id(), course);
        return course;
    }

    public Optional<Course> find(long id) {
        return Optional.ofNullable(courses.get(id));
    }

    public Optional<Course> replace(long id, CourseRequest request) {
        if (!courses.containsKey(id)) {
            return Optional.empty();
        }
        Course course = new Course(id, request.title(), request.price());
        courses.put(id, course);
        return Optional.of(course);
    }

    public boolean delete(long id) {
        return courses.remove(id) != null;
    }

    public PageResponse<Course> page(int page, int size) {
        List<Course> all = new ArrayList<>(courses.values());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResponse<>(all.subList(from, to), page, size, all.size());
    }

    public void clear() {
        courses.clear();
        ids.set(0);
    }
}
