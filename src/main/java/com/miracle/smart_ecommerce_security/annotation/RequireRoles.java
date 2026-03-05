package com.miracle.smart_ecommerce_security.annotation;

import java.lang.annotation.*;

/**
 * @deprecated Replaced by Spring Security {@code @PreAuthorize} / {@code @Secured}.
 * All controllers and GraphQL resolvers now use standard Spring Security method security.
 * Kept for reference only.
 */
@Deprecated
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRoles {
    String[] value();
}
