package com.miracle.smart_ecommerce_security.config;

import io.swagger.v3.oas.models.Components;                              // NEW
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;           // NEW
import io.swagger.v3.oas.models.security.SecurityScheme;               // NEW
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;
import io.swagger.v3.oas.models.Operation;


import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart E-Commerce API")
                        .version("1.0.0")
                        .description("A production-ready e-commerce REST API built with Spring Boot and raw JDBC. " +
                                "This API provides comprehensive endpoints for user management, product catalog, " +
                                "shopping cart, and order processing."))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.smartecommerce.com")
                                .description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token from POST /api/auth/login. Swagger will automatically add 'Authorization: Bearer <token>'")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }



    @Bean
    public OperationCustomizer roleTagCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            org.springframework.security.access.prepost.PreAuthorize preAuthorize =
                    handlerMethod.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            if (preAuthorize == null) {
                preAuthorize = handlerMethod.getBeanType()
                        .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            }
            if (preAuthorize != null) {
                // Extract role info from the expression e.g. "hasRole('ADMIN')" → "[ADMIN]"
                String expr = preAuthorize.value()
                        .replace("hasRole('", "").replace("hasAnyRole('", "")
                        .replace("')", "").replace("', '", ", ");
                if (operation.getSummary() != null) {
                    operation.setSummary(operation.getSummary() + " [" + expr + "]");
                }
            } else {
                io.swagger.v3.oas.annotations.security.SecurityRequirements noAuth =
                        handlerMethod.getMethodAnnotation(io.swagger.v3.oas.annotations.security.SecurityRequirements.class);
                if (noAuth != null && operation.getSummary() != null) {
                    operation.setSummary(operation.getSummary() + " [PUBLIC]");
                }
            }
            return operation;
        };
    }

    /**
     * Patches the generated OpenAPI schema for Spring Data's {@code Pageable} type so that
     * Swagger UI shows the correct default values in the JSON example body:
     * <pre>
     * {
     *   "page": 0,
     *   "size": 10,
     *   "sort": ["createdAt,asc"]
     * }
     * </pre>
     */
    @Bean
    public OpenApiCustomizer pageableSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Schema<?> pageableSchema = openApi.getComponents().getSchemas().get("Pageable");
            if (pageableSchema == null) {
                return;
            }

            // Fix "page" default → 0
            Schema<?> pageField = (Schema<?>) pageableSchema.getProperties().get("page");
            if (pageField != null) {
                pageField.setDefault(0);
                pageField.setDescription("Zero-based page index (0..N)");
            }

            // Fix "size" default → 10
            Schema<?> sizeField = (Schema<?>) pageableSchema.getProperties().get("size");
            if (sizeField != null) {
                sizeField.setDefault(10);
                sizeField.setDescription("Number of results per page. Default: 10, Max: 100");
            }

            // Fix "sort" default → ["createdAt,asc"]
            Schema<?> sortField = (Schema<?>) pageableSchema.getProperties().get("sort");
            if (sortField != null) {
                sortField.setDefault(List.of("createdAt,asc"));
                sortField.setDescription(
                    "Sorting in format: property,(asc|desc). " +
                    "Multiple sort criteria are supported. Default: createdAt,asc"
                );
            }
        };
    }
}