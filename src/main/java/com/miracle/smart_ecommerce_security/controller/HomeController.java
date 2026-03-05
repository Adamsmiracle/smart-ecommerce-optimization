package com.miracle.smart_ecommerce_security.controller;

import java.time.OffsetDateTime;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Home/Health check endpoints.
 */
@RestController
@RequestMapping("/")
@Tag(name = "Home", description = "Home and health check APIs")
public class HomeController {

    @GetMapping
    @Operation(summary = "Home", description = "Returns welcome message and API info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> home() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Smart E-Commerce API");
        info.put("version", "1.0.0");
        info.put("description", "A production-ready e-commerce REST API using raw JDBC");
        info.put("timestamp", OffsetDateTime.now());
        info.put("endpoints", Map.of(
                "users", "/api/users",
                "products", "/api/products",
                "categories", "/api/categories",
                "cart", "/api/cart",
                "orders", "/api/orders",
                "graphql", "/graphql",
                "swagger-ui", "/swagger-ui.html",
                "api-docs", "/v3/api-docs"
        ));
        return ResponseEntity.ok(ApiResponse.success(info, "Welcome to Smart E-Commerce API"));
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Returns API health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", OffsetDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}

