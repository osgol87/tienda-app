package com.speedsneakers.productserviceelastic.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Centraliza las respuestas de error de ProductServiceElastic: un producto
 * inexistente, una petición de alta o edición inválida, o una falla al hablar
 * con Elasticsearch responden con un cuerpo consistente en vez de un 500
 * genérico de Spring Boot.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Cubre tanto IllegalProductIdException (ID vacío) como InvalidProductRequest
     * (datos incompletos al crear o actualizar un producto): ambas extienden
     * IllegalArgumentException y ya traen un mensaje explicativo propio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * El índice de productos vive en Bonsai, un Elasticsearch gestionado fuera
     * del backend. Una caída de red o del propio índice no debe traducirse en
     * un 500 sin contexto para quien está consultando o modificando el catálogo.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleElasticsearchFailure(DataAccessException ex) {
        log.error("Fallo al comunicarse con Elasticsearch: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "No se pudo completar la operación con el catálogo de productos"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        log.error("Error no controlado en ProductServiceElastic", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", ex.getMessage()));
    }
}
