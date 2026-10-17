package dev.canhdominich.spring.mvc;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(int id) {
        super("Course " + id + " does not exist");
    }
}
