package dev.canhdominich.boot.rest;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(long id) {
        super("Course " + id + " does not exist");
    }
}
