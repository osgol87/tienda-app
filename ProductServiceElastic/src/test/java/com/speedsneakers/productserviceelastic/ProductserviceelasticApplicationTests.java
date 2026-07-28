package com.speedsneakers.productserviceelastic;

import com.speedsneakers.productserviceelastic.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"elasticsearch.host=localhost:9200",
		"elasticsearch.user=test",
		"elasticsearch.password=test"
})
class ProductserviceelasticApplicationTests {

	@MockBean
	private ProductRepository productRepository;

	@MockBean
	private ElasticsearchOperations elasticsearchOperations;

	@Test
	void contextLoads() {
	}

}
