package com.miracle.smart_ecommerce_jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartEcommerceJpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartEcommerceJpaApplication.class, args);
	}

}
