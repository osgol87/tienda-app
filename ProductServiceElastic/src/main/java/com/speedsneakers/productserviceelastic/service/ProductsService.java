package com.speedsneakers.productserviceelastic.service;

import com.speedsneakers.productserviceelastic.model.dto.ProductDto;
import com.speedsneakers.productserviceelastic.model.request.ProductRequest;

import java.util.List;

/**
 * Interfaz del servicio de productos.
 */
public interface ProductsService {

    /**
     * Obtiene productos que coincidan con los filtros proporcionados.
     *
     * @param search El término de búsqueda para filtrar productos por nombre, marca o categoría.
     *
     * @return Una lista de productos que coinciden con los filtros.
     */
    List<ProductDto> getProducts(String search);

    /**
     * Obtiene un producto por su ID.
     */
    ProductDto getProductById(String id);

    /**
     * Crea un nuevo producto.
     */
    ProductDto createProduct(ProductRequest request);

    /**
     * Actualiza un producto existente.
     */
    ProductDto updateProduct(String id, ProductRequest request);

    /**
     * Elimina un producto por su ID.
     */
    void deleteProduct(String id);

}
