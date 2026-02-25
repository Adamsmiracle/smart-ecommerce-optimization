package com.miracle.smart_ecommerce_jpa.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Custom transaction annotation with predefined settings for the application.
 * Provides consistent transaction handling across all service methods.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
public @interface CustomTransactional {

    /**
     * Alias for {@link Transactional#rollbackFor()}.
     */
    @AliasFor(annotation = Transactional.class)
    Class<? extends Throwable>[] rollbackFor() default {Exception.class};

    /**
     * Alias for {@link Transactional#propagation()}.
     */
    @AliasFor(annotation = Transactional.class)
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Alias for {@link Transactional#readOnly()}.
     */
    @AliasFor(annotation = Transactional.class)
    boolean readOnly() default false;

    /**
     * Alias for {@link Transactional#timeout()}.
     */
    @AliasFor(annotation = Transactional.class)
    int timeout() default 30;
}
