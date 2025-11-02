package com.space.munova.notification.service;

import com.space.munova.notification.common.NotificationMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {

    // SseEmitter 객체 생성
    SseEmitter createSseEmitter(Object emitterKey);

    // 알림 전송
    // - 생성된 sseEmitter로 전송
    void sendNotification(SseEmitter sseEmitter, Object emitterId, NotificationMessage data);

    // 알림 전송
    // - emitterKey에 해당하는 모든 sseEmitter에 전송
    void sendNotification(Object emitterKey, NotificationMessage data);
}
