package com.speedsneakers.productserviceelastic.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedsneakers.productserviceelastic.model.entity.Product;
import com.speedsneakers.productserviceelastic.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Carga automáticamente el catálogo de productos de referencia al arrancar el servicio.
 *
 * Reemplaza el proceso manual de sembrado del índice de Elasticsearch (hasta ahora hecho a
 * mano con una colección de Postman): si el índice {@code products} está vacío al iniciar la
 * aplicación, se carga el catálogo definido en {@code products-seed.json}. Si el índice ya
 * tiene productos, no se hace nada, para no duplicar datos en cada reinicio.
 */
@Slf4j
@Component
public class ProductCatalogSeeder implements ApplicationRunner {

    /**
     * Archivo, dentro del classpath, con el catálogo de productos de referencia.
     */
    private static final String SEED_FILE = "products-seed.json";

    /**
     * Repositorio de productos.
     */
    private final ProductRepository productRepository;

    /**
     * Mapper usado para deserializar el archivo de sembrado.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor del sembrador de catálogo.
     *
     * @param productRepository Repositorio de productos.
     * @param objectMapper Mapper usado para deserializar el archivo de sembrado.
     */
    @Autowired
    public ProductCatalogSeeder(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Carga el catálogo de productos de referencia si el índice está vacío.
     *
     * Cualquier error al leer el archivo de sembrado o al conectar con Elasticsearch se
     * registra en el log, pero no impide que el resto de la aplicación arranque con
     * normalidad.
     *
     * @param args Argumentos de arranque de la aplicación.
     */
    @Override
    public void run(ApplicationArguments args) {

        try {
            if (productRepository.count() > 0) {
                log.info("El índice de productos ya contiene datos; se omite la carga automática del catálogo");
                return;
            }

            List<Product> seedProducts = loadSeedProducts();
            productRepository.saveAll(seedProducts);
            log.info("Catálogo de productos cargado automáticamente: {} productos insertados", seedProducts.size());
        } catch (Exception e) {
            log.error("No se pudo cargar automáticamente el catálogo de productos", e);
        }
    }

    /**
     * Lee y deserializa el archivo de sembrado del catálogo.
     *
     * @return Lista de productos definidos en el archivo de sembrado.
     *
     * @throws IOException Si el archivo no existe o no se puede leer.
     */
    private List<Product> loadSeedProducts() throws IOException {

        try (InputStream inputStream = new ClassPathResource(SEED_FILE).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<Product>>() { });
        }
    }
}
