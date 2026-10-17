package dev.canhdominich.hibernate.advanced;

public record CourseSearch(String titleContains, String level, Integer minPrice, Integer maxPrice) {

    public static CourseSearch none() {
        return new CourseSearch(null, null, null, null);
    }
}
