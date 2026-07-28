package com.speedsneakers.orderservice.repository;

import com.speedsneakers.orderservice.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para manejar las operaciones CRUD de Order.
 * Extiende JpaRepository para proporcionar métodos predefinidos.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByOrderDateDesc(String userId);
}
