package com.miracle.smart_ecommerce_security.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL runtime wiring configuration.
 *
 * Registers the custom scalars declared in schema.graphqls:
 *   - UUID          → graphql-java-extended-scalars UUID coercing
 *   - BigDecimal    → graphql-java-extended-scalars BigDecimal coercing
 *   - OffsetDateTime → graphql-java-extended-scalars DateTime coercing
 *
 * Without this bean, Spring GraphQL will start but immediately fail schema
 * introspection with "No scalar found for UUID / BigDecimal / OffsetDateTime",
 * which shows up in GraphiQL as "Error fetching schema".
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.DateTime);        // maps to OffsetDateTime
    }
}

