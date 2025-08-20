package com.speedsneakers.productserviceelastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductserviceelasticApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductserviceelasticApplication.class, args);
	}

}
