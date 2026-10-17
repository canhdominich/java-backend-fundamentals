package dev.canhdominich.boot.tx;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = "lessons")
    @Query("select c from Course c")
    List<Course> findAllWithGraph();

    @Query("select distinct c from Course c join fetch c.lessons")
    List<Course> findAllJoinFetch();
}
