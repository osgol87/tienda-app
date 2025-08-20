package com.speedsneakers.orderservice.service;

import com.speedsneakers.orderservice.client.ProductClient;
import com.speedsneakers.orderservice.exception.OrderNotFoundException;
import com.speedsneakers.orderservice.model.dto.OrderDto;
import com.speedsneakers.orderservice.model.dto.OrderItemDto;
import com.speedsneakers.orderservice.model.dto.ProductResponseDto;
import com.speedsneakers.orderservice.model.entity.Order;
import com.speedsneakers.orderservice.model.entity.OrderItem;
import com.speedsneakers.orderservice.model.entity.OrderStatus;
import com.speedsneakers.orderservice.model.request.OrderItemRequestModel;
import com.speedsneakers.orderservice.model.request.OrderRequestModel;
import com.speedsneakers.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de ordenes.
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /**
     * Repositorio de ordenes
     */
    private final OrderRepository orderRepository;

    /**
     * Cliente para comunicarse con el servicio de productos.
     */
    private final ProductClient productClient;

    /**
     * Constructor del servicio de ordenes
     * @param orderRepository Repositorio de ordenes
     * @param productClient Cliente para comunicarse con el servicio de productos
     */
    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, ProductClient productClient) {

        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    /**
     * Crea una nueva orden.
     *
     * @param orderRequest Datos de la orden a crear.
     * @return Detalles de la orden.
     */
    @Override
    @Transactional
    public OrderDto createOrder(OrderRequestModel orderRequest) {

        log.info("Creando una nueva orden con los datos: {}", orderRequest);

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequestModel itemRequest : orderRequest.getOrderItems()) {

            // Validamos que el producto exista en el servicio de productos.
            ProductResponseDto productResponse = productClient.getProductById(itemRequest.getProductId());

            OrderItem orderItem = getOrderItem(itemRequest, productResponse, totalAmount);

            // Centralizamos la lógica de la relación en un solo metodo.
            order.addOrderItem(orderItem);
        }

        order.setTotalAmount(
                order.getOrderItems().stream()
                        .map(OrderItem::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        orderRepository.save(order);

        return convertToDtoWithDetails(order);
    }

    /**
     * Crea un OrderItem a partir de la solicitud y la respuesta del producto.
     *
     * @param itemRequest Modelo con los datos del item de la orden
     * @param productResponse Modelo con los datos del producto
     * @param totalAmount Monto total acumulado de la orden
     * @return OrderItem con los datos del item de la orden
     */
    private static OrderItem getOrderItem(OrderItemRequestModel itemRequest, ProductResponseDto productResponse, BigDecimal totalAmount) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(itemRequest.getProductId());
        orderItem.setName(productResponse.getName());
        orderItem.setQuantity(itemRequest.getQuantity());
        orderItem.setPricePerUnit(productResponse.getPrice());
        orderItem.setImageUrl(productResponse.getImageUrl());

        BigDecimal subtotal = totalAmount.add(
                productResponse.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
        );

        orderItem.setSubtotal(subtotal);
        return orderItem;
    }

    /**
     * Obtiene una orden por su identificador.
     *
     * @param id Identificador de la orden.
     * @return Detalles de la orden.
     */
    @Override
    public OrderDto getOrderById(String id) {

        if (!StringUtils.hasLength(id)) {
            throw new IllegalArgumentException("El ID de la orden no puede estar vacío");
        }

        Optional<Order> optionalOrder = orderRepository.findById(Long.valueOf(id));

        if (optionalOrder.isEmpty()) {
            log.warn("Orden con ID {} no encontrada", id);
            throw new OrderNotFoundException(id);
        }

        return convertToDtoWithDetails(optionalOrder.get());
    }

    /**
     * Obtiene todas las órdenes.
     *
     * @return Lista de todas las órdenes.
     */
    @Override
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"))
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * Convierte una entidad Order a un DTO OrderDto.
     *
     * @param order Entidad Order a convertir.
     * @return OrderDto con los datos de la orden.
     */
    private OrderDto convertToDto(Order order) {

        return new OrderDto(
                order.getId(),
                order.getOrderDate(),
                order.getStatus().toString(),
                order.getTotalAmount()
        );
    }

    /**
     * Convierte una entidad Order a un DTO OrderDto.
     *
     * @param order Entidad Order a convertir.
     * @return OrderDto con los datos de la orden.
     */
    private OrderDto convertToDtoWithDetails(Order order) {

        OrderDto orderDto = new OrderDto(
                order.getId(),
                order.getOrderDate(),
                order.getStatus().toString(),
                order.getTotalAmount()
        );

        for (OrderItem item : order.getOrderItems()) {
            OrderItemDto itemRequest = new OrderItemDto(
                    item.getId(),
                    item.getProductId(),
                    item.getName(),
                    item.getQuantity(),
                    item.getPricePerUnit(),
                    item.getSubtotal(),
                    item.getImageUrl()
            );
            // Añadimos el item a la orden DTO
            orderDto.addOrderItem(itemRequest);
        }

        return orderDto;
    }
}
