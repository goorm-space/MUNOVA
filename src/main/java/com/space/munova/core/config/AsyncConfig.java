package com.space.munova.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * 회원가입용 task
     */
    @Bean(name = "signupExecutor")
    public Executor signupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setThreadNamePrefix("executor-signup-");
        executor.initialize();
        return executor;
    }

    /**
     * Redis Stream 로그 전송용 비동기 Executor
     * 
     * 10,000+ 동시 사용자 처리 시 로그 전송이 메인 스레드를 블로킹하지 않도록
     * 별도 스레드 풀에서 비동기 처리
     * 
     * 설정:
     * - Core Pool Size: 10 (기본 유지 스레드)
     * - Max Pool Size: 50 (최대 스레드, 부하 시 확장)
     * - Queue Capacity: 10,000 (큐 크기, 버퍼 역할)
     * 
     * 동작:
     * - 로그 전송 작업이 큐에 쌓이고, 스레드 풀에서 순차 처리
     * - 메인 API 스레드는 로그 전송 완료를 기다리지 않고 즉시 반환
     */
    @Bean(name = "logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);           // 기본 유지 스레드 수
        executor.setMaxPoolSize(50);            // 최대 스레드 수 (부하 시 확장)
        executor.setQueueCapacity(10000);        // 큐 크기 (10,000개 로그 버퍼)
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 종료 시 대기
        executor.setAwaitTerminationSeconds(60); // 종료 대기 시간 (60초)
        executor.setThreadNamePrefix("executor-log-");  // 스레드 이름 prefix
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()  // 큐 가득 찰 시 호출 스레드에서 실행
        );
        executor.initialize();
        return executor;
    }
}
