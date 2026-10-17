package dev.canhdominich.shop.product;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    Optional<Product> findByIdAndActiveTrue(Long id);

    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    @Modifying
    @Query("update Product p set p.stock = p.stock - :quantity, p.version = p.version + 1 where p.id = :id and p.active = true and p.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :quantity, p.version = p.version + 1 where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
