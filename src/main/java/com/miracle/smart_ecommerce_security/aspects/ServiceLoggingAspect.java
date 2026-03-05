package com.miracle.smart_ecommerce_security.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Pointcut("within(com.miracle.smart_ecommerce_security..service..*)")
    public void serviceLayer() {}

    @Before("serviceLayer()")
    public void beforeAdvice(JoinPoint joinPoint) {
        log.info("Entering in Method : {} with arguments = {}", joinPoint.getSignature().toShortString(), joinPoint.getArgs());
    }

    @After("serviceLayer()")
    public void afterAdvice(JoinPoint joinPoint) {
        log.info("Exiting from Method : {}", joinPoint.getSignature().toShortString());
    }

    @Around("serviceLayer()")
    public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("Method {} executed in {} ms", pjp.getSignature().toShortString(), elapsed);
            return result;
        } catch (Throwable t) {
            log.error("Exception in method {}: {}", pjp.getSignature().toShortString(), t.getMessage());
            throw t;
        }
    }
}

