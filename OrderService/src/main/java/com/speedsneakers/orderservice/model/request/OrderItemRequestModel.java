package com.speedsneakers.orderservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import java.math.BigDecimal;

/**
 * Modelo de datos de la solicitud de creación de un item de la orden.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestModel {

    /**
     * Identificador del producto
     */
    @NotNull(message = "El identificador del producto es requerido")
    private String productId;

    /**
     * Cantidad del producto
     */
    @NotNull(message = "La cantidad del producto es requerida")
    @Min(value = 1, message = "La cantidad del producto debe ser mayor a 0")
    @Valid
    private Integer quantity;

    /**
     * Representación en cadena del item de la orden.
     *
     * @return Cadena que representa el objeto OrderItemRequestModel.
     */
    @Override
    public String toString() {
        return "{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                "}";
    }
}
