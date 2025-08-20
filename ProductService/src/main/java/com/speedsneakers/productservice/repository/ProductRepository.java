package com.speedsneakers.productservice.repository;

import com.speedsneakers.productservice.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para manejar las operaciones CRUD de productos.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Busca productos que coincidan con los filtros proporcionados.
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR :search = '') OR " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchProducts(@Param("search") String search);
}
