package com.miracle.smart_ecommerce_security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartEcommerceSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartEcommerceSecurityApplication.class, args);
	}

}
