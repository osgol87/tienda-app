package com.speedsneakers.orderservice.client;

import com.speedsneakers.orderservice.model.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "productservice")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponseDto getProductById(@PathVariable String id);
}
