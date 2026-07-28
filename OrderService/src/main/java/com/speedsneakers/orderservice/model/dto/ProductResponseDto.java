package com.speedsneakers.orderservice.model.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de respuesta para la información del producto.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    /**
     * Identificador del producto
     */
    private String id;

    /**
     * Nombre del producto
     */
    private String name;

    /**
     * Marca del producto
     */
    private String brand;

    /**
     * Categoría del producto
     */
    private String category;

    /**
     * Descripción corta del producto
     */
    private String shortDescription;

    /**
     * Descripción larga del producto
     */
    private String longDescription;

    /**
     * Precio del producto
     */
    private BigDecimal price;

    /**
     * Imagen del producto
     */
    private String imageUrl;

    /**
     * Representación en cadena del objeto ProductResponseDto.
     *
     * @return Cadena que representa el objeto ProductResponseDto.
     */
    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", name='" + name + "'" +
                ", brand='" + brand + "'" +
                ", category='" + category + "'" +
                ", shortDescription='" + shortDescription + "'" +
                ", longDescription='" + longDescription + "'" +
                ", price=" + price +
                ", imageUrl='" + imageUrl + "'" +
                '}';
    }
}
