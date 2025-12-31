package com.src.ap.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Configuration to ensure Flyway runs AFTER Hibernate DDL.
 * This allows Hibernate to create the main tables (like occupations)
 * before Flyway creates audit triggers that depend on those tables.
 */
@Configuration
public class FlywayConfig {

    /**
     * Configure Flyway to depend on EntityManagerFactory.
     * This ensures Hibernate creates tables before Flyway migrations run.
     */
    @Bean(initMethod = "migrate")
    @DependsOn("entityManagerFactory")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }
}
