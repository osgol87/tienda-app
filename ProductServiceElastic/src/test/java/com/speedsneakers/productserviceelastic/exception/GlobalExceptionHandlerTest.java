package com.speedsneakers.productserviceelastic.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleProductNotFound_devuelve404ConElMensaje() {
        ResponseEntity<Map<String, String>> response =
                handler.handleProductNotFound(new ProductNotFoundException("1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Product not found with ID: 1");
    }

    @Test
    void handleIllegalArgument_cubreIdDeProductoInvalido() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalProductIdException(""));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handleIllegalArgument_cubrePeticionDeProductoInvalida() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new InvalidProductRequest("Invalid product request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid product request");
    }

    @Test
    void handleElasticsearchFailure_devuelve503() {
        ResponseEntity<Map<String, String>> response =
                handler.handleElasticsearchFailure(new DataAccessResourceFailureException("Bonsai no responde"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody())
                .containsEntry("message", "No se pudo completar la operación con el catálogo de productos");
    }

    @Test
    void handleRuntime_devuelve500ConElMensaje() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Error inesperado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Error inesperado");
    }
}
