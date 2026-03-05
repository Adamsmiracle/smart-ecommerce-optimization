package com.miracle.smart_ecommerce_security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SwaggerMvcConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        try {
            converters.removeIf(c -> c instanceof ByteArrayHttpMessageConverter);
            converters.add(0, new ByteArrayHttpMessageConverter());
        } catch (Exception ex) {
            // ignore - best effort
        }

        // Ensure Jackson converter is present for proper JSON serialization
        boolean hasJackson = converters.stream().anyMatch(c -> c instanceof MappingJackson2HttpMessageConverter);
        if (!hasJackson) {
            converters.add(new MappingJackson2HttpMessageConverter());
        }
    }
}

