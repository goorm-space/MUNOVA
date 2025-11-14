package com.space.munova.recommend.infra;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisStreamProducer {
    private final LogBatchBuffer logBuffer;

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
        StreamType(String key) { this.key = key; }
    }

    public void sendLogAsync(StreamType streamType, Map<String, Object> logData) {
        Map<String, Object> redisData = new HashMap<>();

        // 공통 필드 보강
        redisData.put("event_time", Instant.now().toString());
        redisData.put("session_id", UUID.randomUUID().toString());
        redisData.put("version", 1);
        redisData.put("stream_key", streamType.getKey());

        // 평탄화 처리
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

        logBuffer.add(redisData);
    }
}