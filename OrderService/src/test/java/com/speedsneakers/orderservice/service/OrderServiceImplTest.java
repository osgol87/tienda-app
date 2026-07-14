package com.speedsneakers.orderservice.service;

import com.speedsneakers.orderservice.client.ProductClient;
import com.speedsneakers.orderservice.exception.OrderNotFoundException;
import com.speedsneakers.orderservice.model.dto.OrderDto;
import com.speedsneakers.orderservice.model.dto.ProductResponseDto;
import com.speedsneakers.orderservice.model.entity.Order;
import com.speedsneakers.orderservice.model.entity.OrderStatus;
import com.speedsneakers.orderservice.model.request.OrderItemRequestModel;
import com.speedsneakers.orderservice.model.request.OrderRequestModel;
import com.speedsneakers.orderservice.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productClient);
    }

    private ProductResponseDto buildProductResponse(String id, BigDecimal price) {
        ProductResponseDto product = new ProductResponseDto();
        product.setId(id);
        product.setName("Zapatilla Speed");
        product.setPrice(price);
        product.setImageUrl("http://example.com/img.png");
        return product;
    }

    private OrderRequestModel buildRequest(String productId, int quantity) {
        OrderItemRequestModel item = new OrderItemRequestModel();
        item.setProductId(productId);
        item.setQuantity(quantity);

        OrderRequestModel request = new OrderRequestModel();
        request.setOrderItems(List.of(item));
        return request;
    }

    @Test
    void createOrder_calculaElSubtotalYElTotalDeLaOrden() {
        when(productClient.getProductById("1")).thenReturn(buildProductResponse("1", BigDecimal.valueOf(100)));

        OrderDto result = orderService.createOrder(buildRequest("1", 3), "user-1");

        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING.toString());
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getOrderItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(300));

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_sumaElTotalDeVariosItems() {
        OrderItemRequestModel item1 = new OrderItemRequestModel();
        item1.setProductId("1");
        item1.setQuantity(2);
        OrderItemRequestModel item2 = new OrderItemRequestModel();
        item2.setProductId("2");
        item2.setQuantity(1);

        OrderRequestModel request = new OrderRequestModel();
        request.setOrderItems(List.of(item1, item2));

        when(productClient.getProductById("1")).thenReturn(buildProductResponse("1", BigDecimal.valueOf(50)));
        when(productClient.getProductById("2")).thenReturn(buildProductResponse("2", BigDecimal.valueOf(20)));

        OrderDto result = orderService.createOrder(request, "user-1");

        assertThat(result.getOrderItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(120));
    }

    @Test
    void createOrder_propagaFeignExceptionSiElProductoNoExiste() {
        Request feignRequest = Request.create(Request.HttpMethod.GET, "/products/99",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
        FeignException.NotFound notFound = new FeignException.NotFound(
                "Not Found", feignRequest, null, Collections.emptyMap());

        when(productClient.getProductById("99")).thenThrow(notFound);

        assertThatThrownBy(() -> orderService.createOrder(buildRequest("99", 1), "user-1"))
                .isSameAs(notFound);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_devuelveLaOrdenConSusItemsSiPerteneceAlUsuario() {
        Order order = new Order(LocalDateTime.now(), OrderStatus.PENDING, BigDecimal.TEN);
        order.setId(1L);
        order.setUserId("user-1");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById("1", "user-1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo("user-1");
    }

    @Test
    void getOrderById_lanzaExcepcionSiElIdEstaVacio() {
        assertThatThrownBy(() -> orderService.getOrderById("", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void getOrderById_lanzaExcepcionSiElIdNoEsNumerico() {
        assertThatThrownBy(() -> orderService.getOrderById("abc", "user-1"))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void getOrderById_lanzaOrderNotFoundSiNoExiste() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("99", "user-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrderById_lanzaOrderNotFoundSiPerteneceAOtroUsuario() {
        Order order = new Order(LocalDateTime.now(), OrderStatus.PENDING, BigDecimal.TEN);
        order.setId(1L);
        order.setUserId("otro-usuario");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("1", "user-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrdersByUserId_devuelveLasOrdenesDelUsuarioSinItems() {
        Order order = new Order(LocalDateTime.now(), OrderStatus.DELIVERED, BigDecimal.valueOf(150));
        order.setId(5L);
        order.setUserId("user-1");

        when(orderRepository.findByUserIdOrderByOrderDateDesc("user-1")).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getOrdersByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
        assertThat(result.get(0).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void getOrdersByUserId_devuelveListaVaciaSiNoTieneOrdenes() {
        when(orderRepository.findByUserIdOrderByOrderDateDesc("user-1")).thenReturn(List.of());

        List<OrderDto> result = orderService.getOrdersByUserId("user-1");

        assertThat(result).isEmpty();
    }
}
