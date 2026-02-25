package com.miracle.smart_ecommerce_jpa.config;

import io.swagger.v3.oas.models.Components;                              // NEW
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;           // NEW
import io.swagger.v3.oas.models.security.SecurityScheme;               // NEW
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.customizers.OperationCustomizer;
import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import org.springframework.web.method.HandlerMethod;
import io.swagger.v3.oas.models.Operation;
import java.util.Arrays;


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
                        .addSecuritySchemes("X-Auth-Token", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-Token")
                                .description("Paste the token from POST /api/auth/login response. Format: userId:role:uuid")))
                .addSecurityItem(new SecurityRequirement().addList("X-Auth-Token"));
    }



    @Bean
    public OperationCustomizer roleTagCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            RequireRoles roles = handlerMethod.getMethodAnnotation(RequireRoles.class);
            if (roles == null) {
                roles = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
            }
            if (roles != null) {
                String roleList = String.join(", ", roles.value());
                operation.setSummary(operation.getSummary() + " [" + roleList + "]");
            } else {
                operation.setSummary(operation.getSummary() + " [PUBLIC]");
            }
            return operation;
        };
    }
}