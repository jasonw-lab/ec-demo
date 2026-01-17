package com.demo.ec.order.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import io.seata.saga.engine.config.DbStateMachineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Profile("saga")
public class SagaStateMachineConfig {
    private static final Logger log = LoggerFactory.getLogger(SagaStateMachineConfig.class);

    @Bean
    public DbStateMachineConfig dbStateMachineConfig(@Qualifier("sagaDataSource") DataSource sagaDataSource) {
        // Log the SAGA datasource connection details for debugging
        if (sagaDataSource instanceof HikariDataSource) {
            // DataSource is managed by Spring, no need to close
            @SuppressWarnings("resource")
            HikariDataSource hikariDataSource = (HikariDataSource) sagaDataSource;
            log.info("=== DbStateMachineConfig using SAGA DataSource ===");
            log.info("JDBC URL: {}", hikariDataSource.getJdbcUrl());
            log.info("Database: {}", extractDatabaseName(hikariDataSource.getJdbcUrl()));
        }
        
        DbStateMachineConfig cfg = new DbStateMachineConfig();
        cfg.setDataSource(sagaDataSource);
        cfg.setResources(new String[]{"statelang/*.json"});
        cfg.setEnableAsync(true);
        cfg.setThreadPoolExecutor(threadExecutor());
        log.info("DbStateMachineConfig initialized with resources: statelang/*.json");
        return cfg;
    }
    
    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "unknown";
        }
        // Extract database name from JDBC URL like: jdbc:mysql://host:port/database?params
        int dbStart = jdbcUrl.lastIndexOf('/');
        if (dbStart > 0) {
            int dbEnd = jdbcUrl.indexOf('?', dbStart);
            if (dbEnd > 0) {
                return jdbcUrl.substring(dbStart + 1, dbEnd);
            } else {
                return jdbcUrl.substring(dbStart + 1);
            }
        }
        return "unknown";
    }

    @Bean
    public StateMachineEngine stateMachineEngine(DbStateMachineConfig cfg) {
        ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
        engine.setStateMachineConfig(cfg);
        return engine;
    }

    @Bean
    public ThreadPoolExecutor threadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("SAGA_ASYNC_EXE_");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }
}


