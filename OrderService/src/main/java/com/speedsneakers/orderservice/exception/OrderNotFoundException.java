package com.speedsneakers.orderservice.exception;

/**
 * Exception cuando una orden no es encontrada en la base de datos.
 * El código de estado HTTP lo decide GlobalExceptionHandler, no esta clase.
 */
public class OrderNotFoundException extends RuntimeException {

    /**
     * Crea una nueva instancia de OrderNotFoundException con el ID de la orden no encontrada.
     * @param orderId El ID de la orden no encontrada.
     */
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
    }
}
