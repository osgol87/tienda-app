package com.speedsneakers.productserviceelastic.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Modelo de datos de la solicitud de creación de un producto.
 */
@Data
public class ProductRequest {

    /**
     * Nombre del producto.
     */
    @NotNull(message = "El nombre del producto es requerido")
    private String name;

    /**
     * Marca del producto.
     */
    @NotNull(message = "La marca del producto es requerida")
    private String brand;

    /**
     * Categoría del producto.
     */
    @NotNull(message = "La categoría del producto es requerida")
    private String category;

    /**
     * Descripción corta del producto.
     */
    @NotNull(message = "La descripción corta del producto es requerida")
    private String shortDescription;

    /**
     * Descripción larga del producto.
     */
    @NotNull(message = "La descripción larga del producto es requerida")
    private String longDescription;

    /**
     * Precio del producto.
     */
    @NotNull(message = "El precio del producto es requerido")
    private String price;

    /**
     * Imagen del producto.
     */
    @NotNull(message = "La imagen del producto es requerida")
    private String imageUrl;

}
