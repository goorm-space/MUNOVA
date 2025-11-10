package com.space.munova.recommend.infra;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class LogBatchBuffer {

    // 모든 서비스 계층에서 공통으로 접근할 수 있는 로그 버퍼
    private final Queue<Map<String, Object>> buffer = new ConcurrentLinkedQueue<>();

    public void add(Map<String, Object> log) {
        buffer.add(log);
    }

    public Queue<Map<String, Object>> getBuffer() {
        return buffer;
    }
}