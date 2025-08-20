package com.speedsneakers.productserviceelastic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

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
    private Double price;

    /**
     * URL de la imagen del producto
     */
    private String imageUrl;

    /**
     * Representación en cadena del producto.
     *
     * @return Cadena que representa el producto.
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
