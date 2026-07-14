package com.speedsneakers.productserviceelastic.service;

import com.speedsneakers.productserviceelastic.exception.IllegalProductIdException;
import com.speedsneakers.productserviceelastic.exception.InvalidProductRequest;
import com.speedsneakers.productserviceelastic.exception.ProductNotFoundException;
import com.speedsneakers.productserviceelastic.model.dto.ProductDto;
import com.speedsneakers.productserviceelastic.model.entity.Product;
import com.speedsneakers.productserviceelastic.model.request.ProductRequest;
import com.speedsneakers.productserviceelastic.repository.DataAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private DataAccessRepository dataAccessRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(dataAccessRepository);
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId("1");
        product.setName("Zapatilla Speed");
        product.setBrand("SpeedSneakers");
        product.setCategory("Running");
        product.setShortDescription("Ligera");
        product.setLongDescription("Zapatilla ligera para correr");
        product.setPrice(1200.0);
        product.setImageUrl("http://example.com/img.png");
        return product;
    }

    private ProductRequest buildValidRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("Zapatilla Speed");
        request.setBrand("SpeedSneakers");
        request.setCategory("Running");
        request.setShortDescription("Ligera");
        request.setLongDescription("Zapatilla ligera para correr");
        request.setPrice("1200.0");
        request.setImageUrl("http://example.com/img.png");
        return request;
    }

    @Test
    void getProducts_conBusquedaUsaSearchProducts() {
        when(dataAccessRepository.searchProducts("speed")).thenReturn(List.of(buildProduct()));

        List<ProductDto> result = productService.getProducts("speed");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Zapatilla Speed");
        verify(dataAccessRepository).searchProducts("speed");
        verify(dataAccessRepository, never()).findAll();
    }

    @Test
    void getProducts_sinBusquedaUsaFindAll() {
        when(dataAccessRepository.findAll()).thenReturn(List.of(buildProduct()));

        List<ProductDto> result = productService.getProducts(null);

        assertThat(result).hasSize(1);
        verify(dataAccessRepository).findAll();
        verify(dataAccessRepository, never()).searchProducts(any());
    }

    @Test
    void getProducts_conBusquedaEnBlancoUsaFindAll() {
        when(dataAccessRepository.findAll()).thenReturn(List.of());

        productService.getProducts("   ");

        verify(dataAccessRepository).findAll();
        verify(dataAccessRepository, never()).searchProducts(any());
    }

    @Test
    void getProductById_devuelveElProductoEncontrado() {
        when(dataAccessRepository.findById("1")).thenReturn(Optional.of(buildProduct()));

        ProductDto result = productService.getProductById("1");

        assertThat(result.getId()).isEqualTo("1");
        assertThat(result.getBrand()).isEqualTo("SpeedSneakers");
    }

    @Test
    void getProductById_lanzaExcepcionSiElIdEstaVacio() {
        assertThatThrownBy(() -> productService.getProductById(""))
                .isInstanceOf(IllegalProductIdException.class);

        verify(dataAccessRepository, never()).findById(any());
    }

    @Test
    void getProductById_lanzaExcepcionSiNoExisteElProducto() {
        when(dataAccessRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById("99"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createProduct_creaElProductoConDatosValidos() {
        when(dataAccessRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto result = productService.createProduct(buildValidRequest());

        assertThat(result.getName()).isEqualTo("Zapatilla Speed");
        assertThat(result.getPrice()).isEqualTo(1200.0);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(dataAccessRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualTo(1200.0);
    }

    @Test
    void createProduct_lanzaExcepcionSiFaltaUnCampoRequerido() {
        ProductRequest request = buildValidRequest();
        request.setName(null);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(InvalidProductRequest.class);

        verify(dataAccessRepository, never()).save(any());
    }

    @Test
    void createProduct_lanzaExcepcionSiLaPeticionEsNula() {
        assertThatThrownBy(() -> productService.createProduct(null))
                .isInstanceOf(InvalidProductRequest.class);
    }

    @Test
    void updateProduct_actualizaElProductoExistente() {
        Product existing = buildProduct();
        when(dataAccessRepository.findById("1")).thenReturn(Optional.of(existing));
        when(dataAccessRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest request = buildValidRequest();
        request.setName("Zapatilla Speed Pro");

        ProductDto result = productService.updateProduct("1", request);

        assertThat(result.getName()).isEqualTo("Zapatilla Speed Pro");
        verify(dataAccessRepository).save(existing);
    }

    @Test
    void updateProduct_lanzaExcepcionSiFaltaUnCampoRequerido() {
        ProductRequest request = buildValidRequest();
        request.setBrand("");

        assertThatThrownBy(() -> productService.updateProduct("1", request))
                .isInstanceOf(InvalidProductRequest.class);

        verify(dataAccessRepository, never()).findById(any());
    }

    @Test
    void updateProduct_lanzaExcepcionSiElProductoNoExiste() {
        when(dataAccessRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct("99", buildValidRequest()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteProduct_borraElProductoEncontrado() {
        Product existing = buildProduct();
        when(dataAccessRepository.findById("1")).thenReturn(Optional.of(existing));

        productService.deleteProduct("1");

        verify(dataAccessRepository).delete(existing);
    }

    @Test
    void deleteProduct_lanzaExcepcionSiElProductoNoExiste() {
        when(dataAccessRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct("99"))
                .isInstanceOf(ProductNotFoundException.class);

        verify(dataAccessRepository, never()).delete(any());
    }
}
