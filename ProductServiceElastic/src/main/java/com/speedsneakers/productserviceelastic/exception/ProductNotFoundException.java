package com.speedsneakers.productserviceelastic.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception cuando un producto no es encontrado en la base de datos.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Product not found")
public class ProductNotFoundException extends RuntimeException {

    /**
     * Crea una nueva instancia de ProductNotFoundException con el ID del producto inválido.
     * @param productId El ID del producto inválido.
     */
    public ProductNotFoundException(String productId) {
        super("Product not found with ID: " + productId);
    }

}
