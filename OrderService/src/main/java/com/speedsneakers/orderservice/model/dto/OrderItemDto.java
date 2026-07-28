package com.speedsneakers.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    /**
     * Identificador del item de la orden.
     */
    private Long id;

    /**
     * Identificador del producto.
     */
    private String productId;

    /**
     * Nombre del producto.
     */
    private String name;

    /**
     * Cantidad del producto.
     */
    private Integer quantity;

    /**
     * Precio por unidad del producto.
     */
    private BigDecimal pricePerUnit;

    /**
     * Subtotal del item de la orden (quantity * pricePerUnit).
     */
    private BigDecimal subtotal;

    /**
     * URL de la imagen del producto.
     */
    private String imageUrl;

    /**
     * Convierte el item de la orden a una representación de cadena.
     *
     * @return String representation of the OrderItemDto.
     */
    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", productId=" + productId +
                ", name='" + name + "'" +
                ", quantity=" + quantity +
                ", pricePerUnit=" + pricePerUnit +
                ", subtotal=" + subtotal +
                ", imageUrl='" + imageUrl + "'" +
                '}';
    }
}
