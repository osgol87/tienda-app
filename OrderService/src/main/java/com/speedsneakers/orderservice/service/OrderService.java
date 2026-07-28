package com.speedsneakers.orderservice.service;

import com.speedsneakers.orderservice.model.dto.OrderDto;
import com.speedsneakers.orderservice.model.request.OrderRequestModel;

import java.util.List;

/**
 * Interfaz del servicio de órdenes.
 * Proporciona métodos para crear y obtener órdenes.
 */
public interface OrderService {

    /**
     * Crea una nueva orden.
     *
     * @param orderRequest Datos de la orden a crear.
     * @param userId       Identificador del usuario autenticado.
     * @return Detalles de la orden.
     */
    OrderDto createOrder(OrderRequestModel orderRequest, String userId);

    /**
     * Obtiene una orden por su identificador, verificando que pertenezca al usuario.
     *
     * @param orderId Identificador de la orden.
     * @param userId  Identificador del usuario autenticado.
     * @return Detalles de la orden.
     */
    OrderDto getOrderById(String orderId, String userId);

    /**
     * Obtiene todas las órdenes del usuario autenticado.
     *
     * @param userId Identificador del usuario autenticado.
     * @return Lista de órdenes del usuario.
     */
    List<OrderDto> getOrdersByUserId(String userId);
}
