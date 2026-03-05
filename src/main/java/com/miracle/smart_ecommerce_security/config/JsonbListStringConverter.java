package com.miracle.smart_ecommerce_security.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * JPA converter to handle JSONB column type for List<String> fields.
 * Converts between List<String> and JSON string for database storage.
 */
@Converter
@Component
@Slf4j
public class JsonbListStringConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        log.debug("Converting List<String> to database column: {}", attribute);
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        
        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.debug("Converted to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting List<String> to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to List<String>: {}", e.getMessage());
            return List.of();
        }
    }
}
