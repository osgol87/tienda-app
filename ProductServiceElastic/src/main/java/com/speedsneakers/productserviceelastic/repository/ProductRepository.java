package com.speedsneakers.productserviceelastic.repository;

import com.speedsneakers.productserviceelastic.model.entity.Product;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {

    /**
     * Busca productos que coincidan con el filtro proporcionado usando Elasticsearch.
     */
    @Query("""
    {
      "query_string": {
        "query": "*?0*",
        "fields": ["name", "brand", "category"]
      }
    }
    """)
    List<Product> searchProducts(String search);

}
