package com.speedsneakers.productserviceelastic.exception;

/**
 * Exception cuando se recibe un Id de producto inválido.
 * El código de estado HTTP lo decide GlobalExceptionHandler, no esta clase.
 */
public class IllegalProductIdException extends IllegalArgumentException {

    /**
     * Crea una nueva instancia de IllegalProductIdException con el ID del producto inválido.
     * @param productId El ID del producto inválido.
     */
    public IllegalProductIdException(String productId) {
        super("Invalid product ID " + productId);
    }

}
