package dev.canhdominich.spring.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LocalHandlingController {

    @GetMapping("/local/course/{id}")
    @ResponseBody
    public String course(@PathVariable int id) {
        throw new CourseNotFoundException(id);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    @ResponseBody
    public String handledHere(CourseNotFoundException e) {
        return "handled inside the controller: " + e.getMessage();
    }
}
