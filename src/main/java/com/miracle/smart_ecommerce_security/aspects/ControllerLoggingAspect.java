package com.miracle.smart_ecommerce_security.aspects;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for logging HTTP requests and responses at controller layer.
 * Captures request details, execution time, and response status.
 */
@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    /**
     * Pointcut for all REST controller methods
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restControllerMethods() {}

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("execution(* com.miracle.smart_ecommerce_security.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Log HTTP request and response details
     */
    @Around("restControllerMethods() || controllerMethods()")
    public Object logHttpRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);
        String methodName = joinPoint.getSignature().toShortString();

        log.info("HTTP REQUEST - {} {} | Client: {} | Controller: {}",
                method, uri, clientIp, methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("HTTP RESPONSE - {} {} | Status: SUCCESS | Duration: {} ms",
                    method, uri, executionTime);

            if (executionTime > 2000) {
                log.warn("SLOW HTTP REQUEST - {} {} took {} ms", method, uri, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("HTTP RESPONSE - {} {} | Status: ERROR | Duration: {} ms | Error: {}",
                     method, uri, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

