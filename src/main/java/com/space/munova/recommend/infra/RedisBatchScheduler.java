package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RedisBatchScheduler {
    private static final int BATCH_SIZE = 500;
    private static final int STREAM_BUCKETS = 32;
    private static final int MAX_RETRIES = 2; // 최대 재시도 횟수 (부하 테스트를 위해 감소)
    private static final long RETRY_DELAY_MS = 0; // 재시도 간격 (ms) - 즉시 재시도로 변경하여 처리 속도 향상

    private final RedisTemplate<String, Object> redisTemplate;
    private final LogBatchBuffer logBuffer;
    private final MeterRegistry meterRegistry;

    /// Stream Key 캐싱
    private static final byte[][] STREAM_KEY_BYTES = new byte[STREAM_BUCKETS][];

    @PostConstruct
    public void initStreamKeys() {
        for (int i = 0; i < STREAM_BUCKETS; i++) {
            String key = "user_action_stream_" + i;
            STREAM_KEY_BYTES[i] = key.getBytes(StandardCharsets.UTF_8);
        }
    }

    /// Key/Value Byte 변환 헬퍼 (파이프라인용)
    private static byte[] keyBytes(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] valueBytes(Object value) {
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }

    public RedisBatchScheduler(
            @Qualifier("clusterRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            LogBatchBuffer logBuffer,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.logBuffer = logBuffer;
        this.meterRegistry = meterRegistry;
    }

    /// 모니터링
    private Counter batchSendSuccessCounter;
    private Counter batchSendFailureCounter;
    private Counter batchSendTotalCounter;
    private Timer batchSendTimer;

    @PostConstruct
    public void initMetrics() {
        batchSendSuccessCounter = Counter.builder("redis.stream.batch.send.success")
                .description("Redis Stream 배치 전송 성공 횟수")
                .tag("component", "redis_batch_scheduler")
                .register(meterRegistry);

        batchSendFailureCounter = Counter.builder("redis.stream.batch.send.failure")
                .description("Redis Stream 배치 전송 실패 횟수")
                .tag("component", "redis_batch_scheduler")
                .register(meterRegistry);

        batchSendTotalCounter = Counter.builder("redis.stream.batch.send.total")
                .description("Redis Stream 배치 전송 총 메시지 수")
                .tag("component", "redis_batch_scheduler")
                .register(meterRegistry);

        batchSendTimer = Timer.builder("redis.stream.batch.send.duration")
                .description("Redis Stream 배치 전송 소요 시간")
                .tag("component", "redis_batch_scheduler")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 20)
    public void flushBatchToRedis() {
        List<Map<String, Object>> batch = logBuffer.pollBatch(BATCH_SIZE);
        if (batch.isEmpty()) return;

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // 1. 원본 데이터를 Redis 형식으로 변환
            Map<Integer, List<Map<String, Object>>> groupedByBucket = new HashMap<>();
            Instant now = Instant.now();
            long producerTimeMs = System.currentTimeMillis();

            for (Map<String, Object> logData : batch) {
                Map<String, Object> redisData = new HashMap<>();

                redisData.put("event_time", now.toString());
                redisData.put("event_timestamp", String.valueOf(now.toEpochMilli()));
                redisData.put("producer_time", String.valueOf(producerTimeMs));
///                redisData.put("session_id", UUID.randomUUID().toString());
                redisData.put("version", 1);

                // 원본 데이터 변환
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
                Object memberIdObj = redisData.get("member_id");
                int bucket = 0;
                if (memberIdObj != null) {
                    long memberId = Long.parseLong(String.valueOf(memberIdObj));
                    bucket = (int) (memberId % STREAM_BUCKETS);
                }

                groupedByBucket
                        .computeIfAbsent(bucket, k -> new java.util.ArrayList<>())
                        .add(redisData);
            }

            // 각 버킷별로 개별 전송 (파이프라인 제거 - 클러스터 토폴로지 문제로 인한 실패 방지)
            // 파이프라인은 같은 노드로 가는 명령만 묶을 수 있지만, 클러스터 토폴로지 갱신 문제로 실패 가능
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : groupedByBucket.entrySet()) {
                int bucket = entry.getKey();
                List<Map<String, Object>> bucketBatch = entry.getValue();
                String streamKey = "user_action_stream_" + bucket;

                int successCount = 0;
                int failureCount = 0;

                // 개별 전송 (파이프라인 대신) + 재시도 로직
                for (Map<String, Object> redisData : bucketBatch) {
                    Map<String, String> body = new HashMap<>();
                    for (Map.Entry<String, Object> e : redisData.entrySet()) {
                        body.put(e.getKey(), String.valueOf(e.getValue()));
                    }

                    // 재시도 로직
                    boolean sent = false;
                    int retryCount = 0;

                    while (!sent && retryCount < MAX_RETRIES) {
                        try {
                            redisTemplate.opsForStream().add(streamKey, body);
                            successCount++;
                            sent = true;
                        } catch (Exception e) {
                            retryCount++;

                            if (retryCount < MAX_RETRIES) {
                                // 재시도 전 잠시 대기 (토폴로지 갱신 시간 확보)
                                // RETRY_DELAY_MS가 0이면 즉시 재시도하여 처리 속도 향상
                                if (RETRY_DELAY_MS > 0) {
                                    try {
                                        Thread.sleep(RETRY_DELAY_MS);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            } else {
                                // 최종 실패
                                failureCount++;
                                if (failureCount <= 5) {
                                    // 상세 에러 정보 로깅 (근본 원인 파악용)
                                    log.error("Redis 개별 전송 최종 실패 (bucket={}, streamKey={}, 재시도={}): {}",
                                            bucket, streamKey, retryCount, e.getMessage());
                                    log.error("에러 타입: {}, 원인: {}",
                                            e.getClass().getName(),
                                            e.getCause() != null ? e.getCause().getMessage() : "없음");
                                }
                            }
                        }
                    }
                }

                if (successCount > 0) {
                    batchSendSuccessCounter.increment();
                    batchSendTotalCounter.increment(successCount);
                }
                if (failureCount > 0) {
                    batchSendFailureCounter.increment(failureCount);
                    log.error("Redis 배치 전송 부분 실패 (bucket={}, 성공={}, 실패={})",
                            bucket, successCount, failureCount);
                }
            }
        } catch (Exception e) {
            batchSendFailureCounter.increment(batch.size());
            log.warn("Redis 배치 전송 실패: {}", e.getMessage(), e);
        } finally {
            sample.stop(batchSendTimer);
        }
    }
}
