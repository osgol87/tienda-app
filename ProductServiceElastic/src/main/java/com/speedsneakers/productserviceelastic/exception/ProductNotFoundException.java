package com.speedsneakers.productserviceelastic.exception;

/**
 * Exception cuando un producto no es encontrado en la base de datos.
 * El código de estado HTTP lo decide GlobalExceptionHandler, no esta clase.
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Crea una nueva instancia de ProductNotFoundException con el ID del producto inválido.
     * @param productId El ID del producto inválido.
     */
    public ProductNotFoundException(String productId) {
        super("Product not found with ID: " + productId);
    }

}
