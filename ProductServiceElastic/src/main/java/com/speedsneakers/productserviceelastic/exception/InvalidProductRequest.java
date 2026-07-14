package com.speedsneakers.productserviceelastic.exception;

/**
 * Exception cuando se recibe una petición de producto inválida.
 * El código de estado HTTP lo decide GlobalExceptionHandler, no esta clase.
 */
public class InvalidProductRequest extends IllegalArgumentException {

    /**
     * Crea una nueva instancia de InvalidProductRequest con un mensaje de error.
     * @param message El mensaje de error.
     */
    public InvalidProductRequest(String message) {
        super(message);
    }

}
