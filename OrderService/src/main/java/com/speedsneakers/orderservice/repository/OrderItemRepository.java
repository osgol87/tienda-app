package com.speedsneakers.orderservice.repository;

import com.speedsneakers.orderservice.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para manejar las operaciones CRUD de OrderItem.
 * Extiende JpaRepository para proporcionar métodos predefinidos.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
