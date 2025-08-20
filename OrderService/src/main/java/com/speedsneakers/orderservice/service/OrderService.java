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
     * @return Detalles de la orden.
     */
    OrderDto createOrder(OrderRequestModel orderRequest);

    /**
     * Obtiene una orden por su identificador.
     *
     * @param orderId Identificador de la orden.
     * @return Detalles de la orden.
     */
    OrderDto getOrderById(String orderId);

    /**
     * Obtiene todas las órdenes.
     *
     * @return Lista de todas las órdenes.
     */
    List<OrderDto> getAllOrders();
}
