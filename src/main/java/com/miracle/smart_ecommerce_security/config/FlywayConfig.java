package com.miracle.smart_ecommerce_security.config;

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

        // Repair the schema history checksums before migrating.
        // This handles the case where an already-applied migration file was edited
        // (e.g. composite indexes added to V1) — repair updates the stored checksum
        // to match the current file so validate/migrate no longer fails.
        flyway.repair();

        // Run migrations now so they are applied at startup
        flyway.migrate();

        return flyway;
    }
}
