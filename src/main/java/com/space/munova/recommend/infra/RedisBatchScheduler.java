package com.space.munova.recommend.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBatchScheduler {

    @Qualifier("batchRedisTemplate")  // core.config.RedisConfig 에 등록된 배치용 RedisTemplate 사용
    private final RedisTemplate<String, Object> redisTemplate;
    private final LogBatchBuffer logBuffer;

    private static final int BATCH_SIZE = 100;
    private static final String STREAM_KEY = "product_action_stream";

    @Async("logExecutor")
    @Scheduled(fixedDelay = 50)
    public void flushBatchToRedis() {
        List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);

        while (!logBuffer.getBuffer().isEmpty() && batch.size() < BATCH_SIZE) {
            Map<String, Object> polled = logBuffer.getBuffer().poll();
            if (polled != null) batch.add(polled);
        }

        if (batch.isEmpty()) return;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            var streamCommands = connection.streamCommands();
            for (Map<String, Object> logData : batch) {
                Map<byte[], byte[]> body = new HashMap<>();
                for (Map.Entry<String, Object> e : logData.entrySet()) {
                    body.put(
                            e.getKey().getBytes(StandardCharsets.UTF_8),
                            String.valueOf(e.getValue()).getBytes(StandardCharsets.UTF_8)
                    );
                }
                MapRecord<byte[], byte[], byte[]> rec =
                        MapRecord.create(STREAM_KEY.getBytes(StandardCharsets.UTF_8), body);
                streamCommands.xAdd(rec);
            }
            return null;
        });

        log.info("✅ Redis 배치 전송 완료: {}건", batch.size());
    }
}