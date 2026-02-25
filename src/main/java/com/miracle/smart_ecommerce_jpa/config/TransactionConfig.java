package com.miracle.smart_ecommerce_jpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transaction configuration for the application.
 * Configures transaction management and transaction template.
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Transaction template for programmatic transaction management.
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        // Set default timeout to 30 seconds
        template.setTimeout(30);
        // Set default isolation level to READ_COMMITTED
        template.setIsolationLevelName("ISOLATION_READ_COMMITTED");
        return template;
    }
}
