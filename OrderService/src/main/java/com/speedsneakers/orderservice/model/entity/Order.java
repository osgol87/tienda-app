package com.speedsneakers.orderservice.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos de la orden
 */
@Table(name = "orders")
@Entity
@Setter
@Getter
public class Order {

    /**
     * Identificador de la orden
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Fecha de la orden
     */
    @Column(name = "order_date")
    private LocalDateTime orderDate;

    /**
     * Estado de la orden
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * Total de la orden
     */
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Lista de items de la orden
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> orderItems;

    /**
     * Constructor por defecto de la orden.
     */
    public Order() {
        orderItems = new ArrayList<>();
    }

    /**
     * Constructor de la orden con parámetros.
     *
     * @param orderDate   Fecha de la orden.
     * @param status      Estado de la orden.
     * @param totalAmount Total de la orden.
     */
    public Order(LocalDateTime orderDate, OrderStatus status, BigDecimal totalAmount) {
        this();
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * Añade un item a la orden.
     * @param orderItem Item a añadir a la orden.
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * Representación en cadena de la orden.
     * @return Cadena que representa la orden.
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
