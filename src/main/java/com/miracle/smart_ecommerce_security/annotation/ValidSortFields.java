package com.miracle.smart_ecommerce_security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify valid sort fields for a controller method.
 * This provides compile-time validation and documentation for sort parameters.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSortFields {
    
    /**
     * The entity type for which these fields are valid.
     */
    String entityType();
    
    /**
     * Array of valid field names that can be used for sorting.
     */
    String[] fields();
    
    /**
     * Default sort field to use when no sort parameter is provided.
     */
    String defaultField() default "";
    
    /**
     * Default sort direction to use when no direction is specified.
     */
    String defaultDirection() default "ASC";
}
