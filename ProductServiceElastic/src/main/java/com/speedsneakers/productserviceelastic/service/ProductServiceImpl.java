package com.speedsneakers.productserviceelastic.service;

import com.speedsneakers.productserviceelastic.exception.IllegalProductIdException;
import com.speedsneakers.productserviceelastic.exception.InvalidProductRequest;
import com.speedsneakers.productserviceelastic.exception.ProductNotFoundException;
import com.speedsneakers.productserviceelastic.model.dto.ProductDto;
import com.speedsneakers.productserviceelastic.model.entity.Product;
import com.speedsneakers.productserviceelastic.model.request.ProductRequest;
import com.speedsneakers.productserviceelastic.repository.DataAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Servicio de productos.
 */
@Service
public class ProductServiceImpl implements ProductsService {

    /**
     * Repositorio de acceso a datos.
     */
    private final DataAccessRepository dataAccessRepository;

    /**
     * Constructor del servicio de productos.
     *
     * @param dataAccessRepository Repositorio de acceso a datos.
     */
    @Autowired
    public ProductServiceImpl(DataAccessRepository dataAccessRepository) {
        this.dataAccessRepository = dataAccessRepository;
    }

    /**
     * Busca productos
     *
     * @param search El término de búsqueda para filtrar productos por nombre, marca o categoría.
     *
     * @return Lista de productos que coinciden con el término de búsqueda.
     */
    @Override
    public List<ProductDto> getProducts(String search) {

        if (StringUtils.hasText(search)) {

            List<Product> products = dataAccessRepository.searchProducts(search);

            return products.stream()
                    .map(product -> new ProductDto(
                        product.getId(),
                        product.getName(),
                        product.getBrand(),
                        product.getCategory(),
                        product.getShortDescription(),
                        product.getLongDescription(),
                        product.getPrice(),
                        product.getImageUrl()
                    ))
                    .toList();
        } else {

            Iterable<Product> products = dataAccessRepository.findAll();

            return StreamSupport.stream(products.spliterator(), false)
                    .map(product -> new ProductDto(
                            product.getId(),
                            product.getName(),
                            product.getBrand(),
                            product.getCategory(),
                            product.getShortDescription(),
                            product.getLongDescription(),
                            product.getPrice(),
                            product.getImageUrl()
                    ))
                    .toList();
        }
    }

    /**
     * Busca un producto por su ID.
     *
     * @param id Identificador del producto
     */
    @Override
    public ProductDto getProductById(String id) {

        Product product = findProductById(id);

        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getShortDescription(),
                product.getLongDescription(),
                product.getPrice(),
                product.getImageUrl()
        );
    }

    /**
     * Crea un producto
     *
     * @param request Petición de creación de producto
     *
     * @return Producto creado
     *
     * @throws InvalidProductRequest Si la petición de creación de producto no es válida
     */
    @Override
    public ProductDto createProduct(ProductRequest request) {

        if (request != null
            && StringUtils.hasText(request.getName())
            && StringUtils.hasText(request.getBrand())
            && StringUtils.hasText(request.getCategory())
            && StringUtils.hasText(request.getShortDescription())
            && StringUtils.hasText(request.getLongDescription())
            && StringUtils.hasLength(request.getPrice())
            && StringUtils.hasText(request.getImageUrl())
        ) {
            Product product = new Product();
            product.setName(request.getName());
            product.setBrand(request.getBrand());
            product.setCategory(request.getCategory());
            product.setShortDescription(request.getShortDescription());
            product.setLongDescription(request.getLongDescription());
            product.setPrice(Double.valueOf(request.getPrice()));
            product.setImageUrl(request.getImageUrl());

            dataAccessRepository.save(product);

            return new ProductDto(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getShortDescription(),
                product.getLongDescription(),
                product.getPrice(),
                product.getImageUrl()
            );
        }

        throw new InvalidProductRequest("Invalid product request");
    }

    /**
     * Actualiza el registro de un producto
     *
     * @param id Identificador del producto
     * @param request Petición de actualización de producto
     *
     * @return Producto actualizado
     *
     * @throws InvalidProductRequest Si la petición de actualización de producto no es válida
     */
    @Override
    public ProductDto updateProduct(String id, ProductRequest request) {

        if (request != null
            && StringUtils.hasLength(request.getName())
            && StringUtils.hasLength(request.getBrand())
            && StringUtils.hasLength(request.getCategory())
            && StringUtils.hasLength(request.getShortDescription())
            && StringUtils.hasLength(request.getLongDescription())
            && StringUtils.hasLength(request.getPrice())
            && StringUtils.hasLength(request.getImageUrl())
        ) {

            Product product = findProductById(id);

            product.setName(request.getName());
            product.setBrand(request.getBrand());
            product.setCategory(request.getCategory());
            product.setShortDescription(request.getShortDescription());
            product.setLongDescription(request.getLongDescription());
            product.setPrice(Double.valueOf(request.getPrice()));
            product.setImageUrl(request.getImageUrl());

            dataAccessRepository.save(product);

            return new ProductDto(
                    product.getId(),
                    product.getName(),
                    product.getBrand(),
                    product.getCategory(),
                    product.getShortDescription(),
                    product.getLongDescription(),
                    product.getPrice(),
                    product.getImageUrl()
            );
        }

        throw new InvalidProductRequest("Invalid product request");
    }

    /**
     * Borra el registro de un producto
     *
     * @param id Identificador del producto
     */
    @Override
    public void deleteProduct(String id) {

        dataAccessRepository.delete(findProductById(id));
    }

    /**
     * Busca un producto por su ID.
     *
     * @param id Identificador del producto.
     *
     * @return Product Producto encontrado.
     *
     * @throws ProductNotFoundException Si no se encuentra el producto.
     * @throws IllegalProductIdException Si el ID del producto es nulo o vacío.
     */
    private Product findProductById(String id) {

        if (!StringUtils.hasLength(id)) {
            throw new IllegalProductIdException(id);
        }

        return dataAccessRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
