package com.miracle.smart_ecommerce_security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for handling and validating sort parameters throughout the application.
 * Provides centralized validation, sanitization, and conversion of sort parameters.
 */
@Component
@Slf4j
public class SortParameterHandler {

    // Valid sort patterns for different entities
    private static final Map<String, Set<String>> VALID_SORT_FIELDS = new HashMap<>();
    
    // Pattern for valid property names (alphanumeric, dots, underscores)
    private static final Pattern VALID_PROPERTY_PATTERN = Pattern.compile("^[A-Za-z0-9._]+$");
    
    // Pattern for valid directions
    private static final Pattern VALID_DIRECTION_PATTERN = Pattern.compile("^(ASC|DESC|asc|desc)$");
    
    static {
        // User entity valid sort fields
        VALID_SORT_FIELDS.put("User", Set.of(
            "id", "emailAddress", "firstName", "lastName", "phoneNumber", 
            "isActive", "role", "createdAt", "updatedAt"
        ));
        
        // Product entity valid sort fields
        VALID_SORT_FIELDS.put("Product", Set.of(
            "id", "name", "description", "price", "stockQuantity", 
            "isActive", "createdAt", "updatedAt"
        ));
        
        // Category entity valid sort fields
        VALID_SORT_FIELDS.put("Category", Set.of(
            "id", "categoryName", "createdAt", "updatedAt"
        ));
        
        // CustomerOrder entity valid sort fields
        VALID_SORT_FIELDS.put("CustomerOrder", Set.of(
            "id", "orderNumber", "status", "subtotal", "total", 
            "paymentStatus", "createdAt", "updatedAt"
        ));
        
        // OrderItem entity valid sort fields
        VALID_SORT_FIELDS.put("OrderItem", Set.of(
            "id", "unitPrice", "quantity", "totalPrice", "createdAt", "updatedAt"
        ));
        
        // ProductReview entity valid sort fields
        VALID_SORT_FIELDS.put("ProductReview", Set.of(
            "id", "rating", "comment", "createdAt", "updatedAt"
        ));
        
        // PaymentMethod entity valid sort fields
        VALID_SORT_FIELDS.put("PaymentMethod", Set.of(
            "id", "paymentType", "provider", "accountNumber", "isActive", 
            "isDefault", "createdAt", "updatedAt"
        ));
        
        // ShippingMethod entity valid sort fields
        VALID_SORT_FIELDS.put("ShippingMethod", Set.of(
            "id", "name", "description", "price", "estimatedDeliveryDays", 
            "isActive", "createdAt", "updatedAt"
        ));
        
        // Address entity valid sort fields
        VALID_SORT_FIELDS.put("Address", Set.of(
            "id", "addressLine", "city", "region", "country", "postalCode",
            "addressType", "isDefault", "createdAt", "updatedAt"
        ));
        
        // ShoppingCart entity valid sort fields
        VALID_SORT_FIELDS.put("ShoppingCart", Set.of(
            "id", "createdAt", "updatedAt"
        ));
        
        // CartItem entity valid sort fields
        VALID_SORT_FIELDS.put("CartItem", Set.of(
            "id", "quantity", "createdAt", "updatedAt"
        ));
    }
    
    /**
     * Validates and sanitizes sort parameters for a specific entity type.
     * 
     * @param entityType The entity type (e.g., "User", "Product")
     * @param sortParams Raw sort parameters from request
     * @return Sanitized and validated sort parameters
     */
    public Map<String, String[]> validateAndSanitizeSortParams(String entityType, Map<String, String[]> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return sortParams;
        }
        
        Map<String, String[]> sanitizedParams = new HashMap<>();
        Set<String> validFields = VALID_SORT_FIELDS.get(entityType);
        
        if (validFields == null) {
            log.warn("No valid sort fields defined for entity type: {}", entityType);
            return Map.of(); // Return empty map if entity type not recognized
        }
        
        for (Map.Entry<String, String[]> entry : sortParams.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            
            if (paramName.toLowerCase().startsWith("sort")) {
                List<String> sanitizedValues = new ArrayList<>();
                
                for (String value : paramValues) {
                    if (value == null || value.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Parse and validate each sort value
                    String sanitized = sanitizeSortValue(value, validFields);
                    if (sanitized != null) {
                        sanitizedValues.add(sanitized);
                        log.debug("Sanitized sort value: '{}' -> '{}' for entity: {}", value, sanitized, entityType);
                    } else {
                        log.warn("Invalid sort value rejected: '{}' for entity: {}", value, entityType);
                    }
                }
                
                if (!sanitizedValues.isEmpty()) {
                    sanitizedParams.put(paramName, sanitizedValues.toArray(new String[0]));
                }
            } else {
                // Keep non-sort parameters as-is
                sanitizedParams.put(paramName, paramValues);
            }
        }
        
        return sanitizedParams;
    }
    
    /**
     * Creates a Sort object from validated sort parameters.
     * 
     * @param sortValues Array of sort values (e.g., ["name:ASC", "createdAt:DESC"])
     * @return Sort object or null if no valid sort values
     */
    public Sort createSortFromValues(String[] sortValues) {
        if (sortValues == null || sortValues.length == 0) {
            return Sort.unsorted();
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        
        for (String sortValue : sortValues) {
            Sort.Order order = parseSortValue(sortValue);
            if (order != null) {
                orders.add(order);
            }
        }
        
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
    
    /**
     * Parses a single sort value into a Sort.Order.
     * 
     * @param sortValue Sort value string (e.g., "name:ASC" or "name,ASC")
     * @return Sort.Order or null if invalid
     */
    private Sort.Order parseSortValue(String sortValue) {
        if (sortValue == null || sortValue.trim().isEmpty()) {
            return null;
        }
        
        String property;
        String direction = "ASC"; // Default direction
        
        // Handle different formats: "property:direction" or "property,direction"
        if (sortValue.contains(":")) {
            String[] parts = sortValue.split(":", 2);
            property = parts[0].trim();
            direction = parts.length > 1 ? parts[1].trim() : "ASC";
        } else if (sortValue.contains(",")) {
            String[] parts = sortValue.split(",", 2);
            property = parts[0].trim();
            direction = parts.length > 1 ? parts[1].trim() : "ASC";
        } else {
            property = sortValue.trim();
        }
        
        // Check if the "property" is actually just a direction (common error)
        if (VALID_DIRECTION_PATTERN.matcher(property).matches() && !sortValue.contains(":") && !sortValue.contains(",")) {
            log.warn("Invalid sort format: '{}' appears to be only a direction. Expected format: property:direction or property,direction", sortValue);
            return null;
        }
        
        // Validate property name
        if (!VALID_PROPERTY_PATTERN.matcher(property).matches()) {
            log.warn("Invalid property name in sort value: {}", property);
            return null;
        }
        
        // Validate direction
        if (!VALID_DIRECTION_PATTERN.matcher(direction).matches()) {
            log.warn("Invalid direction in sort value: {}", direction);
            return null;
        }
        
        try {
            return new Sort.Order(Sort.Direction.fromString(direction.toUpperCase()), property);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sort direction: {}", direction);
            return null;
        }
    }
    
    /**
     * Sanitizes a single sort value.
     * 
     * @param sortValue Raw sort value
     * @param validFields Set of valid field names for the entity
     * @return Sanitized sort value or null if invalid
     */
    private String sanitizeSortValue(String sortValue, Set<String> validFields) {
        if (sortValue == null || sortValue.trim().isEmpty()) {
            return null;
        }
        
        String property;
        String direction;
        
        // Parse the sort value
        if (sortValue.contains(":")) {
            String[] parts = sortValue.split(":", 2);
            property = parts[0].trim();
            direction = parts.length > 1 ? parts[1].trim() : "ASC";
        } else if (sortValue.contains(",")) {
            String[] parts = sortValue.split(",", 2);
            property = parts[0].trim();
            direction = parts.length > 1 ? parts[1].trim() : "ASC";
        } else {
            property = sortValue.trim();
            direction = "ASC";
        }
        
        // Clean the property name
        String cleanedProperty = cleanPropertyName(property);
        
        // Validate against allowed fields
        if (!validFields.contains(cleanedProperty)) {
            log.warn("Property '{}' not in valid fields for entity: {}", cleanedProperty, validFields);
            return null;
        }
        
        // Validate direction
        if (!VALID_DIRECTION_PATTERN.matcher(direction).matches()) {
            log.warn("Invalid sort direction: {}", direction);
            return null;
        }
        
        return cleanedProperty + ":" + direction.toUpperCase();
    }
    
    /**
     * Cleans a property name by removing invalid characters.
     * 
     * @param property Raw property name
     * @return Cleaned property name
     */
    private String cleanPropertyName(String property) {
        if (property == null) {
            return null;
        }
        
        // Remove quotes, brackets, and other invalid characters
        String cleaned = property.replaceAll("[\"'\\[\\]]", "");
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._]", "");
        cleaned = cleaned.trim();
        
        return cleaned.isEmpty() ? null : cleaned;
    }
    
    /**
     * Gets valid sort fields for an entity type.
     * 
     * @param entityType Entity type name
     * @return Set of valid field names
     */
    public Set<String> getValidSortFields(String entityType) {
        return VALID_SORT_FIELDS.getOrDefault(entityType, Set.of());
    }
    
    /**
     * Checks if a property is valid for sorting for a given entity type.
     * 
     * @param entityType Entity type name
     * @param property Property name to check
     * @return true if valid, false otherwise
     */
    public boolean isValidSortField(String entityType, String property) {
        Set<String> validFields = VALID_SORT_FIELDS.get(entityType);
        return validFields != null && validFields.contains(property);
    }
    
    /**
     * Generates a helpful error message for invalid sort parameters.
     * 
     * @param entityType Entity type name
     * @param invalidValue The invalid sort value that was provided
     * @return Helpful error message with examples and valid fields
     */
    public String generateSortErrorMessage(String entityType, String invalidValue) {
        Set<String> validFields = VALID_SORT_FIELDS.get(entityType);
        if (validFields == null) {
            return String.format("Invalid sort value '%s' for entity type '%s'. No valid sort fields defined.", 
                invalidValue, entityType);
        }
        
        // Check if it's just a direction without property
        if (VALID_DIRECTION_PATTERN.matcher(invalidValue).matches()) {
            return String.format("Invalid sort format: '%s' appears to be only a direction. " +
                "Expected format: property:direction or property,direction. " +
                "Example: name:ASC or createdAt,DESC. " +
                "Valid properties for %s: %s", 
                invalidValue, entityType, String.join(", ", validFields));
        }
        
        return String.format("Invalid sort value '%s' for entity type '%s'. " +
            "Expected format: property:direction or property,direction. " +
            "Example: name:ASC or createdAt,DESC. " +
            "Valid properties for %s: %s", 
            invalidValue, entityType, String.join(", ", validFields));
    }
}
