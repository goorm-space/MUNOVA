package com.space.munova.notification.service;

import com.space.munova.notification.common.NotificationMessage;
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
    public void sendNotification(SseEmitter sseEmitter, Object emitterId, NotificationMessage data) {
        send(sseEmitter, emitterId, data);
    }

    // 알림 전송
    // - emitterKey에 해당하는 모든 sseEmitter에 전송
    @Override
    public void sendNotification(Object emitterKey, NotificationMessage data) {
        List<SseEmitter> emitterList = emitterRepository.findAllEmitterById(emitterKey);
        emitterList.forEach(emitter -> send(emitter, emitterKey, data));
    }

    // 알림 전송
    private void send(SseEmitter emitter, Object emitterId, NotificationMessage data) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(
                    SseEmitter.event()
                            .id(emitterId.toString())
                            .name(data.getNotificationType().name())
                            .data(data)
                            .reconnectTime(emitterReconnectTime)
            );
        } catch (IOException e) {
            log.error("알림 전송 실패: emitterId={}, error={}", emitterId, e.getMessage());
            emitterRepository.delete(emitterId, emitter);
        }
    }
}
