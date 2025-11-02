package com.space.munova.notification.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    // SSE 연결 구독
    SseEmitter subscribe(Long memberId);

    // 알림 발송
    void sendNotification(NotificationPayload payload);

    // 알림 조회
    PagingResponse<NotificationResponse> searchNotifications(Pageable pageable, Sort sort, Long memberId);

    // 알림 읽음 처리
    void markAsRead(Long notificationId);

    // 읽지 않은 알림 개수
    long getUnreadCount(Long memberId);
}
