package com.miracle.smart_ecommerce_security.service;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);
    private final Flyway flyway;

    public void runMigration() {
        flyway.migrate();
        log.info("Migration executed successfully");
    }


    public void showPendingMigrations() {
        MigrationInfoService info = flyway.info();

        for (MigrationInfo migrations: info.pending()){
            log.info("pending: " + migrations.getVersion() + " - " + migrations.getDescription());
        }
    }

}
