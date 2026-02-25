package com.miracle.smart_ecommerce_jpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for custom argument resolvers and other web-related configurations.
 * Using default Spring Data pageable handling with sort parameters disabled at controller level.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // Using default Spring Data pageable handling
}
