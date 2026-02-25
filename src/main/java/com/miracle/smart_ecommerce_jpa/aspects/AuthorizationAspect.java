package com.miracle.smart_ecommerce_jpa.aspects;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.exception.ForbiddenException;
import com.miracle.smart_ecommerce_jpa.exception.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(10)
public class AuthorizationAspect {

    @Around("@annotation(com.miracle.smart_ecommerce_jpa.annotation.RequireRoles) || @within(com.miracle.smart_ecommerce_jpa.annotation.RequireRoles)")
    public Object checkRoles(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();

        RequireRoles ann = method.getAnnotation(RequireRoles.class);
        if (ann == null) {
            ann = pjp.getTarget().getClass().getAnnotation(RequireRoles.class);
        }
        if (ann == null) {
            return pjp.proceed();
        }

        String[] required = ann.value();
        Set<String> requiredSet = Arrays.stream(required).map(String::toUpperCase).collect(Collectors.toSet());

        String userId = MDC.get("userId");
        String role = MDC.get("userRole");

        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }

        if (role == null || role.isBlank()) {
            throw new ForbiddenException("User role missing");
        }

        String normalized = role.toUpperCase();
        if (!requiredSet.contains(normalized)) {
            throw new ForbiddenException("Insufficient role privileges");
        }

        return pjp.proceed();
    }
}

