package com.speedsneakers.productserviceelastic.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception cuando se recibe una petición de producto inválida.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Petición de producto inválida")
public class InvalidProductRequest extends IllegalArgumentException {

    /**
     * Crea una nueva instancia de InvalidProductRequest con un mensaje de error.
     * @param message El mensaje de error.
     */
    public InvalidProductRequest(String message) {
        super(message);
    }

}
