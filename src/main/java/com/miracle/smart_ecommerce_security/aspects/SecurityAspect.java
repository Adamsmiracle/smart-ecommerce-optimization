package com.miracle.smart_ecommerce_security.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect for controller access logging - OPTIMIZED.
 * Disabled by default for production performance.
 */
@Aspect
@Component
@Slf4j
public class SecurityAspect {

    private static final boolean ENABLED = false; // Enable only for debugging

    /**
     * Pointcut for controller methods
     */
    @Pointcut("execution(* com.miracle.smart_ecommerce_security.controller.*.*(..)) || " +
              "execution(* com.miracle.smart_ecommerce_security.domain.*.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Log controller method access attempts - OPTIMIZED
     */
    @Before("controllerMethods()")
    public void logControllerAccess(JoinPoint joinPoint) {
        if (!ENABLED) return;
        String methodName = joinPoint.getSignature().toShortString();
        log.debug("CONTROLLER ACCESS - Method called: {}", methodName);
    }

    /**
     * Log exceptions thrown by controller methods
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logControllerException(JoinPoint joinPoint, Throwable exception) {
        // Always log exceptions (important for debugging)
        String methodName = joinPoint.getSignature().toShortString();
        log.error("CONTROLLER EXCEPTION - Method: {} | Reason: {}", methodName, exception.getMessage());
    }
}
