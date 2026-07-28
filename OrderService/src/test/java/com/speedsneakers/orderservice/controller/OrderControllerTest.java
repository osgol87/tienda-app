package com.speedsneakers.orderservice.controller;

import com.speedsneakers.orderservice.model.dto.OrderDto;
import com.speedsneakers.orderservice.model.request.OrderItemRequestModel;
import com.speedsneakers.orderservice.model.request.OrderRequestModel;
import com.speedsneakers.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderService);
    }

    private OrderDto buildOrderDto() {
        return new OrderDto(1L, LocalDateTime.now(), "PENDING", BigDecimal.valueOf(100), "user-1");
    }

    @Test
    void createOrder_devuelve201ConLaOrdenCreada() {
        OrderItemRequestModel item = new OrderItemRequestModel();
        item.setProductId("1");
        item.setQuantity(2);
        OrderRequestModel request = new OrderRequestModel();
        request.setOrderItems(List.of(item));

        when(orderService.createOrder(request, "user-1")).thenReturn(buildOrderDto());

        ResponseEntity<OrderDto> response = orderController.createOrder(request, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUserId()).isEqualTo("user-1");
    }

    @Test
    void getAllOrders_devuelve200ConLaListaDeOrdenes() {
        when(orderService.getOrdersByUserId("user-1")).thenReturn(List.of(buildOrderDto()));

        ResponseEntity<List<OrderDto>> response = orderController.getAllOrders("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getOrderById_devuelve200ConLaOrden() {
        when(orderService.getOrderById("1", "user-1")).thenReturn(buildOrderDto());

        ResponseEntity<OrderDto> response = orderController.getOrderById("1", "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }
}
