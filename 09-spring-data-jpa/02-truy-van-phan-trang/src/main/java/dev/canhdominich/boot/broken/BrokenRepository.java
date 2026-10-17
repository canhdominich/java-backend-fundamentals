package dev.canhdominich.boot.broken;

import dev.canhdominich.boot.query.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokenRepository extends JpaRepository<Course, Long> {

    List<Course> findByTitel(String title);
}
