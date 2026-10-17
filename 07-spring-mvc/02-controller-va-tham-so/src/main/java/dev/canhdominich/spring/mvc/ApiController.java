package dev.canhdominich.spring.mvc;

import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class ApiController {

    @GetMapping
    public List<StudentDto> all() {
        return List.of(new StudentDto(1, "An"), new StudentDto(2, "Binh"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDto> one(@PathVariable int id) {
        if (id > 2) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new StudentDto(id, id == 1 ? "An" : "Binh"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentDto> create(@RequestBody StudentDto student) {
        StudentDto saved = new StudentDto(3, student.name());
        return ResponseEntity.created(URI.create("/api/students/3")).body(saved);
    }
}
