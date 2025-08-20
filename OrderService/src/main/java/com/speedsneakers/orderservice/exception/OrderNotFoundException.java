package com.speedsneakers.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception cuando una orden no es encontrada en la base de datos.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Order not found")
public class OrderNotFoundException extends RuntimeException {

    /**
     * Crea una nueva instancia de OrderNotFoundException con el ID de la orden no encontrada.
     * @param orderId El ID de la orden no encontrada.
     */
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
    }
}
