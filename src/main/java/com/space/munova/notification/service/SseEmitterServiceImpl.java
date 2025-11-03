package com.space.munova.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.notification.dto.NotificationSseResponse;
import com.space.munova.notification.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterServiceImpl implements SseEmitterService {

    private final EmitterRepository emitterRepository;
    private final ObjectMapper objectMapper;

    @Value("${emitter.timeout}")
    private long emitterTimeout;

    @Value("${emitter.reconnectTime}")
    private long emitterReconnectTime;

    // SseEmitter 객체 생성
    @Override
    public SseEmitter createSseEmitter(Object emitterKey) {
        SseEmitter sseEmitter = new SseEmitter(emitterTimeout);

        // 타임아웃 처리
        sseEmitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: emitterId={}", emitterKey);
            emitterRepository.delete(emitterKey, sseEmitter);
        });

        // 완료 처리
        sseEmitter.onCompletion(() -> {
            log.info("SSE 연결 완료: emitterId={}", emitterKey);
            emitterRepository.delete(emitterKey, sseEmitter);
        });

        // 에러 처리
        sseEmitter.onError(throwable -> {
            log.error("SSE 연결 에러: emitterId={}, error={}", emitterKey, throwable.getMessage());
            emitterRepository.delete(emitterKey, sseEmitter);
        });

        emitterRepository.save(emitterKey, sseEmitter);
        return sseEmitter;
    }

    // 알림 전송
    // - 생성된 sseEmitter로 전송
    @Override
    public void sendNotification(SseEmitter sseEmitter, Object emitterId, NotificationSseResponse response) {
        send(sseEmitter, emitterId, response);
    }

    // 알림 전송
    // - emitterKey에 해당하는 sseEmitter에 전송
    @Override
    public void sendNotification(Object emitterKey, NotificationSseResponse response) {
        List<SseEmitter> emitterList = emitterRepository.findEmitterById(emitterKey);
        emitterList.forEach(emitter -> send(emitter, emitterKey, response));
    }

    // 알림 전송
    private void send(SseEmitter emitter, Object emitterId, NotificationSseResponse response) {
        if (emitter == null) {
            return;
        }
        try {
            String jsonData = objectMapper.writeValueAsString(response);

            emitter.send(
                    SseEmitter.event()
                            .id(emitterId.toString())
                            .name(response.notificationType())
                            .data(jsonData)
                            .reconnectTime(emitterReconnectTime)
            );
        } catch (IOException e) {
            log.error("알림 전송 실패: emitterId={}, error={}", emitterId, e.getMessage());
            emitterRepository.delete(emitterId, emitter);
        }
    }
}
