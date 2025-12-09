package com.space.munova.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "orderExecutor")
    public Executor orderExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores * 2); // 기본 스레드 수
        executor.setMaxPoolSize(executor.getCorePoolSize() * 4); // 요청 폭주 시 늘릴 수 있는 최대 스레드 수
        executor.setQueueCapacity(200); // maxPoolSize까지 늘어나기 전, 대기할 작업 수
        executor.setKeepAliveSeconds(60); // 코어보다 초과한 스레드가 유지되는 시간
        executor.setThreadNamePrefix("orderExecutor-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "paymentExecutor")
    public Executor paymentExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores * 2); // 기본 스레드 수
        executor.setMaxPoolSize(executor.getCorePoolSize() * 4); // 요청 폭주 시 늘릴 수 있는 최대 스레드 수
        executor.setQueueCapacity(200); // maxPoolSize까지 늘어나기 전, 대기할 작업 수
        executor.setKeepAliveSeconds(60); // 코어보다 초과한 스레드가 유지되는 시간
        executor.setThreadNamePrefix("paymentExecutor-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
