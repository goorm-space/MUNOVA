package com.space.munova.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService heartbeatSenderExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService heartbeatCheckerExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService messageBrokerExecutor() {
//        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        return Executors.newFixedThreadPool(1);
    }
}
