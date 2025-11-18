package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Stream 로그 버퍼
 * 
 * 성능 최적화:
 * - AtomicLong으로 크기 추적 (O(n) size() 호출 제거)
 * - Lock-free 동시성 처리
 */
@Getter
@Component
public class LogBatchBuffer {

    private static final int MAX_SIZE = 100_000; // 안전한 최대 큐 크기
    private final Queue<Map<String, Object>> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong currentSize = new AtomicLong(0); // 크기 추적 (O(1) 접근)
    private final MeterRegistry meterRegistry;

    public LogBatchBuffer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initMetrics() {
        // AtomicLong을 사용하여 O(1) 접근으로 성능 최적화
        Gauge.builder("redis.stream.buffer.size", currentSize, AtomicLong::get)
                .description("Redis Stream 버퍼 크기")
                .tag("component", "log_batch_buffer")
                .register(meterRegistry);
        
        Gauge.builder("redis.stream.buffer.capacity", () -> MAX_SIZE)
                .description("Redis Stream 버퍼 최대 크기")
                .tag("component", "log_batch_buffer")
                .register(meterRegistry);
    }

    /**
     * 버퍼에 로그 추가
     * 성능 최적화: AtomicLong으로 크기 추적하여 O(n) size() 호출 제거
     * 
     * @param log 추가할 로그 데이터
     * @return 추가 성공 여부 (버퍼가 가득 찬 경우 false)
     */
    public boolean add(Map<String, Object> log) {
        // O(1) 크기 체크 (size() 대신 AtomicLong 사용)
        long current = currentSize.get();
        if (current >= MAX_SIZE) {
            System.err.println("⚠️ LogQueue 가득참 — 로그 폐기 or DLQ 필요");
            return false;
        }
        
        buffer.add(log);
        currentSize.incrementAndGet();
        return true;
    }

    /**
     * 버퍼에서 로그 제거 (RedisBatchScheduler에서 호출)
     * @return 제거된 로그, 없으면 null
     */
    public Map<String, Object> poll() {
        Map<String, Object> log = buffer.poll();
        if (log != null) {
            currentSize.decrementAndGet();
        }
        return log;
    }

    /**
     * 버퍼가 비어있는지 확인
     * @return 비어있으면 true
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
}
