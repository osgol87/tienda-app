package com.speedsneakers.productserviceelastic.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception cuando se recibe un Id de producto inválido.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid product ID")
public class IllegalProductIdException extends IllegalArgumentException {

    /**
     * Crea una nueva instancia de IllegalProductIdException con el ID del producto inválido.
     * @param productId El ID del producto inválido.
     */
    public IllegalProductIdException(String productId) {
        super("Invalid product ID " + productId);
    }

}
