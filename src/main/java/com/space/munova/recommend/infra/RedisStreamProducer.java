package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamProducer {
    private final LogBatchBuffer logBuffer;
    private final MeterRegistry meterRegistry;

    private Counter logSendSuccessCounter;
    private Counter logSendFailureCounter;
    private Counter logBufferFullCounter;

    // 모니터링 - 나중에 삭제
    @PostConstruct
    public void initMetrics() {
        logSendSuccessCounter = Counter.builder("redis.stream.send.success")
                .description("Redis Stream 로그 전송 성공 횟수")
                .tag("component", "redis_stream_producer")
                .register(meterRegistry);

        logSendFailureCounter = Counter.builder("redis.stream.send.failure")
                .description("Redis Stream 로그 전송 실패 횟수")
                .tag("component", "redis_stream_producer")
                .register(meterRegistry);

        logBufferFullCounter = Counter.builder("redis.stream.buffer.full")
                .description("Redis Stream 버퍼 가득참 횟수")
                .tag("component", "redis_stream_producer")
                .register(meterRegistry);
    }

    public void sendLog(Map<String, Object> logData) {
        try {
            boolean added = logBuffer.add(logData);
            
            if (added) {
                logSendSuccessCounter.increment();
            } else {
                logBufferFullCounter.increment();
            }
        } catch (Exception e) {
            logSendFailureCounter.increment();
            if (System.currentTimeMillis() % 10000 < 100) {
                log.warn("Redis Stream 로그 버퍼 추가 실패: {}", e.getMessage());
            }
        }
    }
}
