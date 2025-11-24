package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis Stream 모니터링 서비스
 * 
 * 데이터 유실 감지:
 * - Stream 길이 (XLEN): 소비되지 않은 메시지 수
 * - Pending 메시지 수: Consumer Group에서 처리 중인 메시지
 * - Stream 길이 증가율: 전송 속도 > 소비 속도면 데이터 유실 위험
 */
@Slf4j
@Component
public class RedisStreamMonitor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    public RedisStreamMonitor(
            @Qualifier("clusterRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    private static final int STREAM_BUCKETS = 10; // RedisBatchScheduler와 동일한 값
    private static final String CONSUMER_GROUP = "recommend-service-group"; // 추천 서버 Consumer Group 이름
    
    private static String[] getStreamKeys() {
        String[] keys = new String[STREAM_BUCKETS];
        for (int i = 0; i < STREAM_BUCKETS; i++) {
            keys[i] = "user_action_stream_" + i;
        }
        return keys;
    }
    
    private static final String[] STREAM_KEYS = getStreamKeys();

    @PostConstruct
    public void initMetrics() {
        // 각 스트림에 대한 Gauge 메트릭 등록
        for (String streamKey : STREAM_KEYS) {
            // Stream 길이 (소비되지 않은 메시지 수)
            Gauge.builder("redis.stream.length", this, monitor -> monitor.getStreamLength(streamKey))
                    .description("Redis Stream 길이 (XLEN) - 소비되지 않은 메시지 수")
                    .tag("stream", streamKey)
                    .register(meterRegistry);
            
            // Pending 메시지 수 (Consumer Group에서 처리 중인 메시지)
            Gauge.builder("redis.stream.pending", this, monitor -> monitor.getPendingCount(streamKey))
                    .description("Redis Stream Pending 메시지 수 (소비 중인 메시지)")
                    .tag("stream", streamKey)
                    .tag("consumer_group", CONSUMER_GROUP)
                    .register(meterRegistry);
        }
    }

    /**
     * 10초마다 Redis Stream 상태를 모니터링
     */
    @Scheduled(fixedDelay = 10000)
    public void monitorStreams() {
        try {
            for (String streamKey : STREAM_KEYS) {
                long length = getStreamLength(streamKey);
                long pending = getPendingCount(streamKey);
                
                if (length > 0 || pending > 0) {
                    log.debug("Stream {} - Length: {}, Pending: {}", streamKey, length, pending);
                }
                
                // 데이터 유실 위험 경고
                if (length > 10000) {
                    log.warn("⚠️ Stream {} 길이가 {}로 증가했습니다. 소비 속도가 전송 속도보다 느립니다.", streamKey, length);
                }
            }
        } catch (Exception e) {
            log.warn("Redis Stream 모니터링 실패: {}", e.getMessage());
        }
    }

    /**
     * Stream 길이 조회 (XLEN) - 소비되지 않은 메시지 수
     */
    public long getStreamLength(String streamKey) {
        try {
            Long length = redisTemplate.opsForStream().size(streamKey);
            return length != null ? length : 0;
        } catch (Exception e) {
            log.debug("Stream {} 길이 조회 실패: {}", streamKey, e.getMessage());
            return 0;
        }
    }

    /**
     * Pending 메시지 수 조회 (Consumer Group에서 처리 중인 메시지)
     */
    public long getPendingCount(String streamKey) {
        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                    .pending(streamKey, CONSUMER_GROUP);
            return summary != null ? summary.getTotalPendingMessages() : 0;
        } catch (Exception e) {
            // Consumer Group이 없을 수 있음 (정상)
            return 0;
        }
    }
}

