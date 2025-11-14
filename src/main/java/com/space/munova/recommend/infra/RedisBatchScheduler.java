package com.space.munova.recommend.infra;

/*
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

    public RedisBatchScheduler(
            @Qualifier("clusterRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            LogBatchBuffer logBuffer
    ) {
        this.redisTemplate = redisTemplate;
        this.logBuffer = logBuffer;
    }

    private static final int BATCH_SIZE = 100;
    private static final int STREAM_BUCKETS = 10; // 10개의 스트림 그룹으로 분산 -> 어차피 redis가 자동으로 할당해줘서 더 잘게 나눠서 한쪽으로 과부하 쏠림 방지

    // 50ms 간격으로 배치 전송
    @Scheduled(fixedDelay = 50)
    public void flushBatchToRedis() {
        List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);

        //버퍼에서 로그 가져옴
        while (!logBuffer.getBuffer().isEmpty() && batch.size() < BATCH_SIZE) {
            Map<String, Object> polled = logBuffer.getBuffer().poll();
            if (polled != null) batch.add(polled);
        }

        if (batch.isEmpty()) return;

        try {
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


                    // memberId 별로 stream을 나눔 10개로 (1001 -> 1, 205 -> 5 ...)
                    Object memberIdObj = logData.get("member_id");
                    String streamKey;
                    if (memberIdObj != null) {
                        long memberId = Long.parseLong(String.valueOf(memberIdObj));
                        int bucket = (int) (memberId % STREAM_BUCKETS); // 예: 0~9
                        streamKey = "user_action_stream_" + bucket;
                    } else {
                        streamKey = "user_action_stream_unknown";
                    }
                    MapRecord<byte[], byte[], byte[]> record =
                            MapRecord.create(streamKey.getBytes(StandardCharsets.UTF_8), body);
                    RecordId recordId=streamCommands.xAdd(record);
                    log.info("✅ XADD 전송 완료: {}건", recordId);
                }
                return null;
            });
            log.info("✅ Redis 배치 전송 완료: {}건", batch.size());
        } catch (Exception e) {
            log.warn("Redis 배치 전송 실패: {}", e.getMessage());
        }
    }
}
 */