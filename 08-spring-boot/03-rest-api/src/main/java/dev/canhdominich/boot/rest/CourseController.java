package dev.canhdominich.boot.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseStore store;

    public CourseController(CourseStore store) {
        this.store = store;
    }

    @GetMapping
    public PageResponse<Course> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size) {
        return store.page(page, size);
    }

    @GetMapping("/{id}")
    public Course one(@PathVariable long id) {
        return store.find(id).orElseThrow(() -> new CourseNotFoundException(id));
    }

    @PostMapping
    public ResponseEntity<Course> create(@Valid @RequestBody CourseRequest request) {
        Course created = store.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public Course replace(@PathVariable long id, @Valid @RequestBody CourseRequest request) {
        return store.replace(id, request).orElseThrow(() -> new CourseNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!store.delete(id)) {
            throw new CourseNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }
}
