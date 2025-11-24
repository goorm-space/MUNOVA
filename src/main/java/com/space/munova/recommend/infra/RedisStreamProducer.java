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

            // 공통 필드 보강 (데이터 유실 및 시간 측정을 위한 타임스탬프)
            Instant now = Instant.now();
            long producerTimeMs = System.currentTimeMillis(); // Producer 전송 시점 타임스탬프 (latency 측정용)
            redisData.put("event_time", now.toString());
            redisData.put("event_timestamp", String.valueOf(now.toEpochMilli())); // 밀리초 단위 타임스탬프 (소비 시간 측정용)
            redisData.put("producer_time", String.valueOf(producerTimeMs)); // Producer 전송 시점 (Consumer latency 계산용)
            redisData.put("session_id", UUID.randomUUID().toString());
            redisData.put("version", 1);
            // stream_key는 RedisBatchScheduler에서 memberId 기반으로 user_action_stream_0~9로 분산

            // 평탄화 처리
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
            if (added) {
                logSendSuccessCounter.increment();
            } else {
                // 큐 가득참 - 로그 폐기 (이미 LogBatchBuffer에서 카운터 증가)
                logBufferFullCounter.increment();
            }
        } catch (Exception e) {
            // 로그 전송 실패가 API 응답에 영향을 주지 않도록 예외 처리
            // 예외 발생 시에도 API 스레드 블로킹 없음
            logSendFailureCounter.increment();
            // 로깅은 샘플링 (과도한 로그 방지)
            if (System.currentTimeMillis() % 10000 < 100) { // 10초마다 한 번만
                log.warn("Redis Stream 로그 전송 실패: {}", e.getMessage());
            }
        }
    }
}
