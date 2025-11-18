package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class RedisBatchScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LogBatchBuffer logBuffer;
    private final MeterRegistry meterRegistry;

    private Counter batchSendSuccessCounter;
    private Counter batchSendFailureCounter;
    private Counter batchSendTotalCounter;
    private Timer batchSendTimer;

    public RedisBatchScheduler(
            @Qualifier("clusterRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            LogBatchBuffer logBuffer,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.logBuffer = logBuffer;
        this.meterRegistry = meterRegistry;
    }

    private static final int BATCH_SIZE = 100;
    private static final int STREAM_BUCKETS = 10; // 10개의 스트림 그룹으로 분산 -> 어차피 redis가 자동으로 할당해줘서 더 잘게 나눠서 한쪽으로 과부하 쏠림 방지

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

    // 50ms 간격으로 배치 전송
    @Scheduled(fixedDelay = 50)
    public void flushBatchToRedis() {
        List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);

        //버퍼에서 로그 가져옴 (최적화된 poll() 메서드 사용)
        while (!logBuffer.isEmpty() && batch.size() < BATCH_SIZE) {
            Map<String, Object> polled = logBuffer.poll();
            if (polled != null) batch.add(polled);
        }

        if (batch.isEmpty()) return;

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // 파이프라인 모드로 배치 전송 (성능 최적화)
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                var streamCommands = connection.streamCommands();

                for (Map<String, Object> logData : batch) {
                    Map<byte[], byte[]> body = new HashMap<>();
                    for (Map.Entry<String, Object> e : logData.entrySet()) {
                        body.put(
                                e.getKey().getBytes(StandardCharsets.UTF_8),
                                String.valueOf(e.getValue()).getBytes(StandardCharsets.UTF_8)
                        );
                    }

                    // RedisStreamProducer에서 설정한 stream_key 사용 (우선순위)
                    // 없으면 memberId 기반으로 bucket 분산
                    String streamKey;
                    Object streamKeyObj = logData.get("stream_key");
                    if (streamKeyObj != null) {
                        // stream_key가 있으면 그대로 사용 (예: "product_action_stream")
                        streamKey = String.valueOf(streamKeyObj);
                    } else {
                        // stream_key가 없으면 memberId 기반 bucket 분산
                        Object memberIdObj = logData.get("member_id");
                        if (memberIdObj != null) {
                            long memberId = Long.parseLong(String.valueOf(memberIdObj));
                            int bucket = (int) (memberId % STREAM_BUCKETS); // 예: 0~9
                            streamKey = "user_action_stream_" + bucket;
                        } else {
                            streamKey = "user_action_stream_unknown";
                        }
                    }
                    
                    MapRecord<byte[], byte[], byte[]> record =
                            MapRecord.create(streamKey.getBytes(StandardCharsets.UTF_8), body);
                    streamCommands.xAdd(record);
                }
                return null;
            });
            // 파이프라인 모드에서는 results가 List로 반환되지만, 실제 성공 여부는 Stream에 데이터가 있는지로 확인
            batchSendSuccessCounter.increment();
            batchSendTotalCounter.increment(batch.size());
            log.info("✅ Redis 배치 전송 완료: {}건 (파이프라인 모드)", batch.size());
        } catch (Exception e) {
            batchSendFailureCounter.increment();
            log.warn("Redis 배치 전송 실패: {}", e.getMessage(), e);
        } finally {
            sample.stop(batchSendTimer);
        }
    }
}
