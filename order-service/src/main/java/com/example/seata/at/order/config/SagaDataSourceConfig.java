package com.example.seata.at.order.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Configure two DataSources under saga profile:
 * - Primary business DataSource (seata_order)
 * - Saga persistence DataSource (seata)
 */
@Configuration
@Profile("saga")
public class SagaDataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SagaDataSourceConfig.class);

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource businessDataSource() {
        DataSource dataSource = DataSourceBuilder.create().build();
        if (dataSource instanceof HikariDataSource) {
            // DataSource is managed by Spring, no need to close
            @SuppressWarnings("resource")
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            log.info("=== Business DataSource (Primary) ===");
            log.info("JDBC URL: {}", hikariDataSource.getJdbcUrl());
            log.info("Username: {}", hikariDataSource.getUsername());
            log.info("Driver Class: {}", hikariDataSource.getDriverClassName());
        }
        return dataSource;
    }

    @Bean(name = "sagaDataSource")
    @ConfigurationProperties(prefix = "spring.saga-datasource")
    public DataSource sagaDataSource() {
        DataSource dataSource = DataSourceBuilder.create().build();
        if (dataSource instanceof HikariDataSource) {
            // DataSource is managed by Spring, no need to close
            @SuppressWarnings("resource")
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            log.info("=== SAGA DataSource (State Machine) ===");
            log.info("JDBC URL: {}", hikariDataSource.getJdbcUrl());
            log.info("Username: {}", hikariDataSource.getUsername());
            log.info("Driver Class: {}", hikariDataSource.getDriverClassName());
        }
        return dataSource;
    }
}
