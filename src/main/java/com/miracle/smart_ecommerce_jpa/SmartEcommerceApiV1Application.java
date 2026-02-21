package com.miracle.smart_ecommerce_jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartEcommerceApiV1Application {

	public static void main(String[] args) {
		SpringApplication.run(SmartEcommerceApiV1Application.class, args);
	}

}
