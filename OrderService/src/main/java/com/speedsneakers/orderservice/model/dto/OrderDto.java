package com.speedsneakers.orderservice.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class OrderDto {

    /**
     * Identificador de la orden.
     */
    private Long id;

    /**
     * Fecha de la orden.
     */
    private LocalDateTime orderDate;

    /**
     * Estado de la orden.
     */
    private String status;

    /**
     * Total de la orden.
     */
    private BigDecimal totalAmount;

    /**
     * Identificador del usuario que creó la orden.
     */
    private String userId;

    /**
     * Lista de items de la orden.
     */
    private List<OrderItemDto> orderItems;

    /**
     * Constructor por defecto de OrderDto.
     * Inicializa la lista de items de la orden.
     */
    public OrderDto() {
        orderItems = new ArrayList<>();
    }

    /**
     * Constructor de OrderDto con parámetros.
     *
     * @param id          Identificador de la orden.
     * @param orderDate   Fecha de la orden.
     * @param status      Estado de la orden.
     * @param totalAmount Total de la orden.
     */
    public OrderDto(Long id, LocalDateTime orderDate, String status, BigDecimal totalAmount) {
        this();
        this.id = id;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public OrderDto(Long id, LocalDateTime orderDate, String status, BigDecimal totalAmount, String userId) {
        this(id, orderDate, status, totalAmount);
        this.userId = userId;
    }

    /**
     * Método para agregar un item a la lista de items de la orden.
     *
     * @param orderItemDto Item de la orden a agregar.
     */
    public void addOrderItem(OrderItemDto orderItemDto) {
        this.orderItems.add(orderItemDto);
    }

    /**
     * Convierte el objeto OrderDto a una representación en cadena.
     *
     * @return Representación en cadena del objeto OrderDto.
     */
    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", orderDate='" + orderDate + "'" +
                ", status='" + status + "'" +
                ", totalAmount=" + totalAmount +
                ", orderItems=" + orderItems +
                '}';
    }
}
