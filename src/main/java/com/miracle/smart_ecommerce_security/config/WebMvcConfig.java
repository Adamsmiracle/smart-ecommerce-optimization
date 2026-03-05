package com.miracle.smart_ecommerce_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for custom argument resolvers and other web-related configurations.
 * Configures global Pageable defaults: page=0, size=10, sort=createdAt ASC.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Customise the global Pageable defaults used by all Spring MVC controllers.
     * - Default page size : 10
     * - Default sort      : createdAt ASC
     * These apply whenever the caller does not supply page/size/sort query parameters,
     * and are overridden per-controller via @PageableDefault where needed.
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(
                org.springframework.data.domain.PageRequest.of(
                    0,
                    10,
                    Sort.by(Sort.Direction.ASC, "createdAt")
                )
            );
            resolver.setMaxPageSize(100);
        };
    }
}
