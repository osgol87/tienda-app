package com.speedsneakers.orderservice.model.entity;

/**
 * Enumeración de los posibles estados de una orden
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
