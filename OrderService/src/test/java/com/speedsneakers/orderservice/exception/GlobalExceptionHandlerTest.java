package com.speedsneakers.orderservice.exception;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Request buildFeignRequest() {
        return Request.create(Request.HttpMethod.GET, "/products/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void handleOrderNotFound_devuelve404ConElMensaje() {
        ResponseEntity<Map<String, String>> response =
                handler.handleOrderNotFound(new OrderNotFoundException("1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Order not found with ID: 1");
    }

    @Test
    void handleValidationErrors_devuelve400ConMapaDeCampos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "orderRequest");
        bindingResult.addError(new FieldError("orderRequest", "orderItems", "La lista de items de la orden es requerida"));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("orderItems", "La lista de items de la orden es requerida");
    }

    @Test
    void handleIllegalArgument_devuelve400ConElMensaje() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("El ID de la orden no puede estar vacío"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "El ID de la orden no puede estar vacío");
    }

    @Test
    void handleNumberFormat_devuelve400ConMensajeFijo() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNumberFormat(new NumberFormatException("For input string: \"abc\""));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "El ID de la orden debe ser numérico");
    }

    @Test
    void handleProductNotFound_devuelve404ConMensajeDeCatalogo() {
        FeignException.NotFound notFound = new FeignException.NotFound(
                "Not Found", buildFeignRequest(), null, Collections.emptyMap());

        ResponseEntity<Map<String, String>> response = handler.handleProductNotFound(notFound);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("message", "Uno de los productos de la orden ya no está disponible en el catálogo");
    }

    @Test
    void handleProductServiceFailure_devuelve503ParaCualquierOtraFallaDeFeign() {
        Request request = buildFeignRequest();
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Collections.emptyMap())
                .build();
        FeignException ex = FeignException.errorStatus("ProductClient#getProductById(String)", response);

        ResponseEntity<Map<String, String>> result = handler.handleProductServiceFailure(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getBody())
                .containsEntry("message", "No se pudo verificar el producto con el servicio de catálogo");
    }

    @Test
    void handleRuntime_devuelve500ConElMensaje() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Error inesperado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Error inesperado");
    }
}
