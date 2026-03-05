package com.miracle.smart_ecommerce_security.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging GraphQL operations.
 * Monitors GraphQL queries, mutations, and their execution times.
 */
@Aspect
@Component
@Slf4j
public class GraphQLLoggingAspect {

    /**
     * Pointcut for GraphQL query methods
     */
    @Pointcut("@annotation(org.springframework.graphql.data.method.annotation.QueryMapping)")
    public void queryMappingMethods() {}

    /**
     * Pointcut for GraphQL mutation methods
     */
    @Pointcut("@annotation(org.springframework.graphql.data.method.annotation.MutationMapping)")
    public void mutationMappingMethods() {}

    /**
     * Monitor GraphQL query execution
     */
    @Around("queryMappingMethods()")
    public Object logGraphQLQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        String queryName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();

        log.info("GRAPHQL QUERY - {} | Args: {}", queryName, formatArgs(args));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("GRAPHQL QUERY SUCCESS - {} | Duration: {} ms", queryName, executionTime);

            if (executionTime > 1000) {
                log.warn("SLOW GRAPHQL QUERY - {} took {} ms", queryName, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("GRAPHQL QUERY ERROR - {} | Duration: {} ms | Error: {}",
                     queryName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Monitor GraphQL mutation execution
     */
    @Around("mutationMappingMethods()")
    public Object logGraphQLMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        String mutationName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();

        log.info("GRAPHQL MUTATION - {} | Args: {}", mutationName, formatArgs(args));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("GRAPHQL MUTATION SUCCESS - {} | Duration: {} ms", mutationName, executionTime);

            if (executionTime > 2000) {
                log.warn("SLOW GRAPHQL MUTATION - {} took {} ms", mutationName, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("GRAPHQL MUTATION ERROR - {} | Duration: {} ms | Error: {}",
                     mutationName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Format arguments for logging (hide sensitive data)
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg.toString().contains("password")) {
                sb.append("[REDACTED]");
            } else {
                String argStr = arg.toString();
                sb.append(argStr.length() > 100 ? argStr.substring(0, 100) + "..." : argStr);
            }
        }

        return sb.toString();
    }
}

