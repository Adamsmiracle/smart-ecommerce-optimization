package com.miracle.smart_ecommerce_jpa.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect for controller access logging (security removed).
 * Previously contained Spring Security-specific logic; now simplified to avoid dependency.
 */
@Aspect
@Component
@Slf4j
public class SecurityAspect {

    /**
     * Pointcut for controller methods
     */
    @Pointcut("execution(* com.miracle.smart_ecommerce_jpa.controller.*.*(..)) || " +
              "execution(* com.miracle.smart_ecommerce_jpa.domain.*.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Log controller method access attempts
     */
    @Before("controllerMethods()")
    public void logControllerAccess(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        log.debug("CONTROLLER ACCESS - Method called: {}", methodName);
    }

    /**
     * Log exceptions thrown by controller methods
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logControllerException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().toShortString();

        log.error("CONTROLLER EXCEPTION - Method: {} | Reason: {}", methodName, exception.getMessage());
    }
}
