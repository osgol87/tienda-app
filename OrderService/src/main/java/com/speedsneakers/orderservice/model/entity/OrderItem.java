package com.speedsneakers.orderservice.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Modelo de datos del item de la orden
 */
@Table(name = "order_items", indexes = @Index(columnList = "product_id"))
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    /**
     * Identificador del item de la orden
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Orden a la que pertenece el item.
     * Por defecto, nombramos la columna como "order_id" para mantener la consistencia con las convenciones de JPA.
     * Esto permite que JPA maneje correctamente la relación entre la orden y sus items.
     * Si se desea cambiar el nombre de la columna, se puede hacer mediante la anotación:
     * @JoinColumn(name = "custom_order_id") en la relación.
     * Sin embargo, es recomendable seguir las convenciones de JPA para evitar confusiones
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonManagedReference
    private Order order;

    /**
     * Identificador del producto
     */
    @Column(name = "product_id")
    private String productId;

    /**
     * Nombre del producto
     */
    @Column(name = "name")
    private String name;

    /**
     * Url de la imagen del producto
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Cantidad del producto
     */
    @Column(name = "quantity")
    private Integer quantity;

    /**
     * Precio unitario del producto
     */
    @Column(name = "price_per_unit", precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    /**
     * Precio unitario del producto
     */
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Representación en cadena del item de la orden.
     * @return Cadena que representa el item de la orden.
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
