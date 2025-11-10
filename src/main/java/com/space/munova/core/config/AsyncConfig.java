package com.space.munova.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);      // 동시에 처리할 로그 스레드 수
        executor.setMaxPoolSize(80);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("LogSender-");
        executor.initialize();
        return executor;
    }
}