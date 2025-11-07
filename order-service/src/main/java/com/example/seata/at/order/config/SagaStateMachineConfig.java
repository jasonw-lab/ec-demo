package com.example.seata.at.order.config;

import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import io.seata.saga.engine.config.DbStateMachineConfig;
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

    @Bean
    public DbStateMachineConfig dbStateMachineConfig(@Qualifier("sagaDataSource") DataSource sagaDataSource) {
        DbStateMachineConfig cfg = new DbStateMachineConfig();
        cfg.setDataSource(sagaDataSource);
        cfg.setResources(new String[]{"statelang/*.json"});
        cfg.setEnableAsync(true);
        cfg.setThreadPoolExecutor(threadExecutor());
        return cfg;
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


