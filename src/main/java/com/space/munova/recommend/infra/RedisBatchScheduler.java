package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class RedisBatchScheduler {
    private static final int BATCH_SIZE = 500;
    private static final int STREAM_BUCKETS = 10;

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
            List<Map<String, Object>> redisDataList = new java.util.ArrayList<>(batch.size());
            Instant now = Instant.now();
            long producerTimeMs = System.currentTimeMillis();

            for (Map<String, Object> logData : batch) {
                Map<String, Object> redisData = new HashMap<>();

                redisData.put("event_time", now.toString());
                redisData.put("event_timestamp", String.valueOf(now.toEpochMilli()));
                redisData.put("producer_time", String.valueOf(producerTimeMs));
                redisData.put("session_id", UUID.randomUUID().toString());
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

                redisDataList.add(redisData);
            }

            // 2. Redis 클러스터에서 파이프라인은 같은 노드로 가는 명령만 묶어야 함
            // 스트림 키별로 그룹화 (원본 데이터와 Redis 데이터를 함께 저장)
            Map<Integer, List<Map.Entry<Map<String, Object>, Map<String, Object>>>> groupedByBucket = new HashMap<>();

            for (int i = 0; i < redisDataList.size(); i++) {
                Map<String, Object> redisData = redisDataList.get(i);
                Map<String, Object> originalLog = batch.get(i); // 원본 데이터 보존

                Object memberIdObj = redisData.get("member_id");
                int bucket = 0;
                if (memberIdObj != null) {
                    long memberId = Long.parseLong(String.valueOf(memberIdObj));
                    bucket = (int) (memberId % STREAM_BUCKETS);
                }
                groupedByBucket.computeIfAbsent(bucket, k -> new java.util.ArrayList<>())
                        .add(Map.entry(originalLog, redisData));
            }

            // 각 버킷별로 파이프라인 실행
            for (Map.Entry<Integer, List<Map.Entry<Map<String, Object>, Map<String, Object>>>> entry : groupedByBucket.entrySet()) {
                int bucket = entry.getKey();
                List<Map.Entry<Map<String, Object>, Map<String, Object>>> bucketBatch = entry.getValue();
                byte[] streamKeyBytes = STREAM_KEY_BYTES[bucket];

                try {
                    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                        var streamCommands = connection.streamCommands();

                        for (Map.Entry<Map<String, Object>, Map<String, Object>> dataEntry : bucketBatch) {
                            Map<String, Object> redisData = dataEntry.getValue();
                            Map<byte[], byte[]> body = new HashMap<>(32);
                            for (Map.Entry<String, Object> e : redisData.entrySet()) {
                                body.put(keyBytes(e.getKey()), valueBytes(e.getValue()));
                            }

                            MapRecord<byte[], byte[], byte[]> record =
                                    MapRecord.create(streamKeyBytes, body);
                            streamCommands.xAdd(record);
                        }
                        return null;
                    });

                    batchSendSuccessCounter.increment();
                    batchSendTotalCounter.increment(bucketBatch.size());
                } catch (Exception e) {
                    batchSendFailureCounter.increment(bucketBatch.size());
                    log.warn("Redis 배치 전송 실패 (bucket={}): {}", bucket, e.getMessage(), e);

                    // ⚠️ 중요: 전송 실패한 메시지를 다시 버퍼에 넣어서 손실 방지
                    for (Map.Entry<Map<String, Object>, Map<String, Object>> dataEntry : bucketBatch) {
                        Map<String, Object> originalLog = dataEntry.getKey();
                        // 원본 데이터를 다시 버퍼에 추가 (버퍼가 가득 차면 실패하지만, 최소한 시도는 함)
                        boolean reAdded = logBuffer.add(originalLog);
                        if (!reAdded) {
                            log.warn("전송 실패한 메시지를 버퍼에 다시 넣지 못했습니다. 버퍼가 가득 찼습니다.");
                        }
                    }
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
