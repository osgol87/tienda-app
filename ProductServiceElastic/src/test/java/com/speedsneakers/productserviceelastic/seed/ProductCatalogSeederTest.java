package com.speedsneakers.productserviceelastic.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedsneakers.productserviceelastic.model.entity.Product;
import com.speedsneakers.productserviceelastic.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogSeederTest {

    @Mock
    private ProductRepository productRepository;

    private ProductCatalogSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new ProductCatalogSeeder(productRepository, new ObjectMapper());
    }

    @Test
    void noCargaElCatalogoCuandoElIndiceYaTieneProductos() {
        when(productRepository.count()).thenReturn(3L);

        seeder.run(null);

        verify(productRepository, never()).saveAll(any());
    }

    @Test
    void cargaElCatalogoDeReferenciaCuandoElIndiceEstaVacio() {
        when(productRepository.count()).thenReturn(0L);

        seeder.run(null);

        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());

        List<Product> seeded = captor.getValue();
        assertThat(seeded).hasSize(10);
        assertThat(seeded).allSatisfy(product -> {
            assertThat(product.getName()).isNotBlank();
            assertThat(product.getBrand()).isNotBlank();
            assertThat(product.getCategory()).isNotBlank();
            assertThat(product.getShortDescription()).isNotBlank();
            assertThat(product.getLongDescription()).isNotBlank();
            assertThat(product.getPrice()).isNotNull();
            assertThat(product.getImageUrl()).isNotBlank();
        });
    }

    @Test
    void noPropagaErroresSiFallaLaConexionConElRepositorio() {
        when(productRepository.count()).thenThrow(new RuntimeException("Elasticsearch no disponible"));

        assertThatCode(() -> seeder.run(null)).doesNotThrowAnyException();

        verify(productRepository, never()).saveAll(any());
    }
}
