package dev.canhdominich.spring.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CourseController {

    @GetMapping("/pages/course/{id}")
    @ResponseBody
    public String course(@PathVariable int id) {
        if (id > 2) {
            throw new CourseNotFoundException(id);
        }
        return "course " + id;
    }
}
