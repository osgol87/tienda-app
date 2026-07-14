package com.speedsneakers.orderservice.controller;

import com.speedsneakers.orderservice.model.dto.OrderDto;
import com.speedsneakers.orderservice.model.request.OrderRequestModel;
import com.speedsneakers.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para manejar las solicitudes relacionadas con las órdenes.
 */
@RestController
@Slf4j
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Constructor del controlador de órdenes.
     *
     * @param orderService Servicio de órdenes.
     */
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Crea una nueva orden.
     *
     * @param orderRequest Datos de la orden a crear.
     * @return Detalles de la orden creada.
     */
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody OrderRequestModel orderRequest,
            @RequestHeader("X-User-Id") String userId) {

        OrderDto order = orderService.createOrder(orderRequest, userId);
        log.info("Orden creada con éxito para usuario {}: {}", userId, order);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Obtiene todas las órdenes del usuario autenticado.
     *
     * @param userId Identificador del usuario desde el header inyectado por el Gateway.
     * @return Lista de órdenes del usuario.
     */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders(
            @RequestHeader("X-User-Id") String userId) {
        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Obtiene una orden por su ID.
     *
     * @param id     ID de la orden a obtener.
     * @param userId Identificador del usuario desde el header inyectado por el Gateway.
     * @return Detalles de la orden.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {

        OrderDto order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(order);
    }
}
