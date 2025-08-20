package com.speedsneakers.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Petición de producto inválida")
public class InvalidProductRequest extends IllegalArgumentException {

    /**
     * Crea una nueva instancia de ProductNotFoundException con el ID del producto inválido.
     * @param productId El ID del producto inválido.
     */
    public InvalidProductRequest(String message) {
        super(message);
    }
}
