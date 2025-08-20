package com.speedsneakers.productserviceelastic.repository;

import com.speedsneakers.productserviceelastic.model.entity.Product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos.
 */
@Repository
public class DataAccessRepository {

    /**
     * Repositorio de productos.
     */
    private final ProductRepository productRepository;

    /**
     * Operaciones Elasticsearch.
     */
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Contructor de la clase
     * @param productRepository Repositorio de productos
     * @param elasticsearchOperations Operaciones Elasticsearch
     */
    @Autowired
    public DataAccessRepository(ProductRepository productRepository, ElasticsearchOperations elasticsearchOperations) {

        this.productRepository = productRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Guarda un registro de producto
     *
     * @param product Producto a guardar
     *
     * @return Producto guardado
     */
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /**
     * Elimina un producto por Id
     *
     * @param id Id del producto
     */
    public void delete(Product product) {

        productRepository.delete(product);
    }

    /**
     * Busca un producto por Id
     *
     * @param id Id del producto
     *
     * @return Producto encontrado
     */
    public Optional<Product> findById(String id) {

        return productRepository.findById(id);
    }

    /**
     * Busca todos los productos
     * @return Lista de productos
     */
    public Iterable<Product> findAll() {

        return productRepository.findAll();
    }

    /**
     * Busca productos que coincidan con el filtro proporcionado
     *
     * @param search Filtro de búsqueda
     *
     * @return List de productos que coinciden con el filtro proporcionado
     */
    public List<Product> searchProducts(String search) {

        return productRepository.searchProducts(search);
    }
}
