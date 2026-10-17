package dev.canhdominich.spring.mvc;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseApi {

    @GetMapping("/api/course/{id}")
    public Map<String, Object> course(@PathVariable int id) {
        if (id > 2) {
            throw new CourseNotFoundException(id);
        }
        return Map.of("id", id);
    }

    @PostMapping("/api/users")
    public Map<String, String> create(@Valid @RequestBody User user) {
        return Map.of("created", user.getName());
    }

    @GetMapping("/api/boom")
    public String boom() {
        throw new IllegalStateException("password for the database is hunter2");
    }
}
