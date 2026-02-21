package com.miracle.smart_ecommerce_jpa.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL Configuration
 * Registers custom scalar types for GraphQL schema
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        // Create BigDecimal scalar with the exact name used in schema
        GraphQLScalarType bigDecimalScalar = GraphQLScalarType.newScalar()
                .name("BigDecimal")
                .coercing(ExtendedScalars.GraphQLBigDecimal.getCoercing())
                .build();

        // Create OffsetDateTime scalar with the exact name used in schema
        GraphQLScalarType offsetDateTimeScalar = GraphQLScalarType.newScalar()
                .name("OffsetDateTime")
                .coercing(ExtendedScalars.DateTime.getCoercing())
                .build();

        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(offsetDateTimeScalar)
                .scalar(bigDecimalScalar)
                .build();
    }
}

