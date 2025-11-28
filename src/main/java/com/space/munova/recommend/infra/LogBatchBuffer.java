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

    private static final int MAX_SIZE = 50_000; // 유동적으로 바꿔야함

    private final BlockingQueue<Map<String, Object>> buffer = new LinkedBlockingQueue<>(MAX_SIZE); // 다른 자료구조는 없나?

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

        //0에 가까워질수록: API 들어오는 속도 > Redis로 빠져나가는 속도
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
        boolean added = buffer.offer(log);

        if (added) {
            currentSize.increment();
            return true;
        } else {
            bufferFullCounter.increment();
            return false;
        }
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


    public int remainingCapacity() {
        return buffer.remainingCapacity();
    }


    public boolean isNearlyFull() {
        return currentSize.intValue() >= MAX_SIZE * 0.9;
    }
}
