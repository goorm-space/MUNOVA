package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redis Stream 로그 전송 Producer
 * 
 * 10,000+ 동시 사용자 처리 최적화:
 * - @Async("logExecutor")로 완전한 비동기 처리
 * - API 요청 스레드가 로그 전송 완료를 기다리지 않음
 * - 별도 스레드 풀에서 로그 데이터 가공 및 버퍼 추가
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamProducer {
    private final LogBatchBuffer logBuffer;
    private final MeterRegistry meterRegistry;
    
    private Counter logSendSuccessCounter;
    private Counter logSendFailureCounter;
    private Counter logBufferFullCounter;
    private Timer logSendTimer;

    @Getter
    public enum StreamType {
        MEMBER("member_action_stream"),
        CHAT("chat_action_stream"),
        PRODUCT("product_action_stream"),
        COUPON("coupon_action_stream"),
        ORDER("order_action_stream"),
        PAYMENT("payment_action_stream"),
        RECOMMEND("recommend_action_stream");

        private final String key;
        StreamType(String key) { this.key = key; }
    }

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
        
        logSendTimer = Timer.builder("redis.stream.send.duration")
                .description("Redis Stream 로그 전송 소요 시간")
                .tag("component", "redis_stream_producer")
                .register(meterRegistry);
    }

    @Async("logExecutor")
    public void sendLogAsync(StreamType streamType, Map<String, Object> logData) {
        Timer.Sample sample = Timer.start(meterRegistry);
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
            redisData.put("stream_key", streamType.getKey()); // 스트림 키 추가

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

            boolean added = logBuffer.add(redisData);
            if (added) {
                logSendSuccessCounter.increment();
            } else {
                logBufferFullCounter.increment();
            }
        } catch (Exception e) {
            // 로그 전송 실패가 API 응답에 영향을 주지 않도록 예외 처리
            logSendFailureCounter.increment();
            log.warn("Redis Stream 로그 전송 실패: {}", e.getMessage(), e);
        } finally {
            sample.stop(logSendTimer);
        }
    }
}
