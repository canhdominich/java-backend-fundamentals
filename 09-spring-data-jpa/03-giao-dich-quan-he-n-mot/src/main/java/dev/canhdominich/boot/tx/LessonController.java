package dev.canhdominich.boot.tx;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LessonController {

    private final CourseService service;

    public LessonController(CourseService service) {
        this.service = service;
    }

    @GetMapping("/lessons/count")
    public String count() {
        return "lessons " + service.lessonsWithoutTransaction();
    }
}
