package com.space.munova.recommend.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.LongAdder;

@Getter
@Component
public class LogBatchBuffer {

    private static final int MAX_SIZE = 10_000;

    private final BlockingQueue<Map<String, Object>> buffer = new LinkedBlockingQueue<>(MAX_SIZE);

    private final LongAdder currentSize = new LongAdder();

    private final MeterRegistry meterRegistry;
    private Counter bufferFullCounter;

    public LogBatchBuffer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initMetrics() {
        // LongAdder를 사용한 Gauge
        Gauge.builder("redis.stream.buffer.size", currentSize, LongAdder::sum)
                .description("Redis Stream 버퍼 크기")
                .tag("component", "log_batch_buffer")
                .register(meterRegistry);

        Gauge.builder("redis.stream.buffer.capacity", () -> MAX_SIZE)
                .description("Redis Stream 버퍼 최대 크기")
                .tag("component", "log_batch_buffer")
                .register(meterRegistry);

        Gauge.builder("redis.stream.buffer.remaining", () -> MAX_SIZE - currentSize.intValue())
                .description("Redis Stream 버퍼 남은 공간")
                .tag("component", "log_batch_buffer")
                .register(meterRegistry);

        bufferFullCounter = Counter.builder("redis.stream.buffer.full")
                .description("Redis Stream 버퍼 가득참으로 인한 로그 폐기 횟수")
                .tag("component", "log_batch_buffer")
                .register(meterRegistry);
    }


    public boolean add(Map<String, Object> log) {
        // offer()는 non-blocking, 즉시 반환
        // 큐가 가득 차면 false 반환 (backpressure - drop 정책)
        boolean added = buffer.offer(log);

        if (added) {
            currentSize.increment();
            return true;
        } else {
            // 큐 가득참 - 로그 폐기 (API 안정성 우선)
            bufferFullCounter.increment();
            return false;
        }
    }

    public Map<String, Object> poll() {
        Map<String, Object> log = buffer.poll();
        if (log != null) {
            currentSize.decrement();
        }
        return log;
    }

    public java.util.List<Map<String, Object>> pollBatch(int batchSize) {
        java.util.List<Map<String, Object>> batch = new java.util.ArrayList<>(batchSize);

        // drainTo() 사용 (한 번에 여러 개 제거, 더 효율적)
        int drained = buffer.drainTo(batch, batchSize);
        if (drained > 0) {
            currentSize.add(-drained);
        }

        return batch;
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public int size() {
        return currentSize.intValue();
    }
}
