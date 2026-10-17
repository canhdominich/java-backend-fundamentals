package dev.canhdominich.boot.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courses;

    public CourseService(CourseRepository courses) {
        this.courses = courses;
    }

    @Transactional(readOnly = true)
    public int lessonsPerCourseLazy() {
        return courses.findAll().stream().mapToInt(course -> course.getLessons().size()).sum();
    }

    @Transactional(readOnly = true)
    public int lessonsPerCourseWithGraph() {
        return courses.findAllWithGraph().stream().mapToInt(course -> course.getLessons().size()).sum();
    }

    @Transactional(readOnly = true)
    public int lessonsPerCourseJoinFetch() {
        return courses.findAllJoinFetch().stream().mapToInt(course -> course.getLessons().size()).sum();
    }

    public int lessonsWithoutTransaction() {
        return courses.findAll().stream().mapToInt(course -> course.getLessons().size()).sum();
    }
}
