package com.speedsneakers.productserviceelastic.controller;

import com.speedsneakers.productserviceelastic.model.dto.ProductDto;
import com.speedsneakers.productserviceelastic.model.request.ProductRequest;
import com.speedsneakers.productserviceelastic.service.ProductsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductsControllerTest {

    @Mock
    private ProductsService productsService;

    private ProductsController productsController;

    @BeforeEach
    void setUp() {
        productsController = new ProductsController(productsService);
    }

    private ProductDto buildDto() {
        return new ProductDto("1", "Zapatilla Speed", "SpeedSneakers", "Running",
                "Ligera", "Zapatilla ligera para correr", 1200.0, "http://example.com/img.png");
    }

    @Test
    void getProducts_conBusquedaDelegaEnElServicio() {
        when(productsService.getProducts("speed")).thenReturn(List.of(buildDto()));

        ResponseEntity<List<ProductDto>> response = productsController.getProducts("speed");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(productsService).getProducts("speed");
    }

    @Test
    void getProducts_sinBusquedaDelegaEnElServicioConNull() {
        when(productsService.getProducts(null)).thenReturn(List.of());

        ResponseEntity<List<ProductDto>> response = productsController.getProducts(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getProduct_devuelve200ConElProducto() {
        when(productsService.getProductById("1")).thenReturn(buildDto());

        ResponseEntity<ProductDto> response = productsController.getProduct("1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo("1");
    }

    @Test
    void addProduct_devuelve201ConElProductoCreado() {
        ProductRequest request = new ProductRequest();
        when(productsService.createProduct(request)).thenReturn(buildDto());

        ResponseEntity<ProductDto> response = productsController.addProduct(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getName()).isEqualTo("Zapatilla Speed");
    }

    @Test
    void updateProduct_devuelve200ConElProductoActualizado() {
        ProductRequest request = new ProductRequest();
        when(productsService.updateProduct("1", request)).thenReturn(buildDto());

        ResponseEntity<ProductDto> response = productsController.updateProduct("1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteProduct_devuelve200YDelegaEnElServicio() {
        ResponseEntity<Void> response = productsController.deleteProduct("1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productsService).deleteProduct("1");
    }

    @Test
    void healthCheck_devuelve200ConMensajeDeEstado() {
        ResponseEntity<String> response = productsController.healthCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Service is up and running");
    }
}
