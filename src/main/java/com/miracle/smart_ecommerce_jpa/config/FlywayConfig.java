package com.miracle.smart_ecommerce_jpa.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                // Match application.yaml baseline-on-migrate: true and baseline-version: 1
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        // Run migrations now so they are applied at startup
        flyway.migrate();

        return flyway;
    }
}
