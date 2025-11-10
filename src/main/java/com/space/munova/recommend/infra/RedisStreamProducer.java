package com.space.munova.recommend.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamProducer {
    private final RedisTemplate<String, Object> redisTemplate;

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

        StreamType(String key) {
            this.key = key;
        }

    }

   // 비동기 처리로
    @Async("logExecutor") // 별도의 스레드 풀로 비동기 실행
    public void sendLogAsync(StreamType streamType, Map<String, Object> logData) {
        try {
            // 필수 공통 필드 보강
            Map<String,String> redisData = new HashMap<>();
            redisData.put("event_time",Instant.now().toString());
            redisData.put("session_id",UUID.randomUUID().toString());
            redisData.put("version","1");

            // object -> string
            for (Map.Entry<String, Object> entry : logData.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> nestedMap) {
                    // 평탄화 처리 (예: data.product_id = 5)
                    for (Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                        redisData.put(entry.getKey() + "." + nestedEntry.getKey(), String.valueOf(nestedEntry.getValue()));
                    }
                } else {
                    redisData.put(entry.getKey(), String.valueOf(value));
                }
            }
            // Redis Stream 전송
            MapRecord<String, String, String> record =
                    MapRecord.create(streamType.getKey(), redisData);

            redisTemplate.opsForStream().add(record);

            log.debug("Redis 로그 전송 완료: {}", redisData);

        } catch (Exception e) {
            // Redis 연결 실패나 전송 실패 시 로직 중단 방지
            log.warn("Redis Stream 전송 실패 (무시됨): {}", e.getMessage());
        }
    }
}