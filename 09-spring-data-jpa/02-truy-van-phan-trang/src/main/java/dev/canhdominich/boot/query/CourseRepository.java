package dev.canhdominich.boot.query;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTitleContainingIgnoreCase(String part);

    List<Course> findByPriceVndBetween(int low, int high);

    List<Course> findByLevelInOrderByPriceVndDesc(Collection<Level> levels);

    List<Course> findTop3ByOrderByPriceVndAsc();

    long countByLevel(Level level);

    boolean existsByTitle(String title);

    @Query("select c from Course c where c.category = :category and c.priceVnd >= :min")
    List<Course> expensiveIn(@Param("category") String category, @Param("min") int min);

    @Query(value = "select * from course where category = ?1 order by price_vnd limit 2", nativeQuery = true)
    List<Course> cheapestTwoNative(String category);

    Page<Course> findByLevel(Level level, Pageable pageable);

    Slice<Course> findSliceByLevel(Level level, Pageable pageable);

    List<Course> findListByLevel(Level level, Pageable pageable);

    List<CourseTitle> findTitlesByLevel(Level level);

    List<CourseSummary> findSummariesByLevel(Level level);

    <T> List<T> findByCategory(String category, Class<T> type);

    @Modifying
    @Query("update Course c set c.priceVnd = c.priceVnd + :delta where c.level = :level")
    int raisePrices(@Param("level") Level level, @Param("delta") int delta);

    @Modifying(clearAutomatically = true)
    @Query("update Course c set c.priceVnd = c.priceVnd + :delta where c.level = :level")
    int raisePricesAndClear(@Param("level") Level level, @Param("delta") int delta);
}
