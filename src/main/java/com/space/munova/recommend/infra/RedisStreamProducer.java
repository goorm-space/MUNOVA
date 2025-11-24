package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public void sendLogAsync(Map<String, Object> logData) {
        try {
            Map<String, Object> redisData = new HashMap<>();

            Instant now = Instant.now();
            long producerTimeMs = System.currentTimeMillis();
            redisData.put("event_time", now.toString());
            redisData.put("event_timestamp", String.valueOf(now.toEpochMilli())); // 실시간/배치 latency 계산
            redisData.put("producer_time", String.valueOf(producerTimeMs)); // api -> producer 단계 측정
            redisData.put("session_id", UUID.randomUUID().toString());
            redisData.put("version", 1);

            for (Map.Entry<String, Object> entry : logData.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> nestedMap) {
                    for (Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                        redisData.put(entry.getKey() + "." + nestedEntry.getKey(), String.valueOf(nestedEntry.getValue()));
                    }
                } else {
                    redisData.put(entry.getKey(), String.valueOf(value));
                }
            }

            // 버퍼에 직접 추가 (non-blocking, offer() 사용)
            boolean added = logBuffer.add(redisData);

            //모니터링 - 나중에 삭제
            if (added) {
                logSendSuccessCounter.increment();
            } else {
                logBufferFullCounter.increment();
            }
        } catch (Exception e) {
            logSendFailureCounter.increment();
            if (System.currentTimeMillis() % 10000 < 100) {
                log.warn("Redis Stream 로그 전송 실패: {}", e.getMessage());
            }
        }
    }
}
