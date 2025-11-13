package com.space.munova.recommend.infra;

import lombok.Getter;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Component
public class LogBatchBuffer {

    private static final int MAX_SIZE = 100_000; // 안전한 최대 큐 크기
    private final Queue<Map<String, Object>> buffer = new ConcurrentLinkedQueue<>();

    public void add(Map<String, Object> log) {
        if (buffer.size() < MAX_SIZE) {
            buffer.add(log);
        } else {
            System.err.println("⚠️ LogQueue 가득참 — 로그 폐기 or DLQ 필요");
        }
    }

}