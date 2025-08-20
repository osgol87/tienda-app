package com.speedsneakers.orderservice.model.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Modelo de datos de la solicitud de creación de una orden.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestModel {

    /**
     * Lista de items de la orden.
     */
    @NotNull(message = "La lista de items de la orden es requerida")
    @Valid
    private List<OrderItemRequestModel> orderItems;

    @Override
    public String toString() {
        return "{" +
                "orderItems=" + orderItems +
                '}';
    }
}
