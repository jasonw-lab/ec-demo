package com.example.seata.at.order.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource businessDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "sagaDataSource")
    @ConfigurationProperties(prefix = "spring.saga-datasource")
    public DataSource sagaDataSource() {
        return DataSourceBuilder.create().build();
    }
}
