package com.space.munova.notification.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationResponse;
import com.space.munova.notification.dto.NotificationSseResponse;
import com.space.munova.notification.dto.NotificationType;
import com.space.munova.notification.entity.Notification;
import com.space.munova.notification.exception.NotificationException;
import com.space.munova.notification.repository.NotificationQueryDslRepository;
import com.space.munova.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static com.space.munova.notification.dto.ConnectNotification.CLIENT_CONNECT;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final SseEmitterService emitterService;
    private final NotificationRepository notificationRepository;
    private final NotificationQueryDslRepository notificationQueryDslRepository;

    // SSE 연결 구독
    @Override
    public SseEmitter subscribe(Long memberId) {
        // SseEmitter 생성
        SseEmitter emitter = emitterService.createSseEmitter(memberId);
        NotificationPayload payload = NotificationPayload.of(
                memberId,
                memberId,
                NotificationType.SYSTEM,
                CLIENT_CONNECT
        );
        // 연결 알림
        NotificationSseResponse response = NotificationSseResponse.from(payload);
        emitterService.sendNotification(emitter, memberId.toString(), response);
        log.info("SSE 구독 성공: emitterId={}", memberId);

        return emitter;
    }

    // 알림 발송
    @Override
    @Transactional
    public void sendNotification(NotificationPayload payload) {
        Notification notification = Notification.from(payload);
        Long notificationId = 0L;

        // DB 저장
        if (payload.type().isShouldSave()) {
            Notification savedNotification = notificationRepository.save(notification);
            notificationId = savedNotification.getId();
        }

        NotificationSseResponse response = NotificationSseResponse.from(notificationId, payload);
        // 알림 전송
        emitterService.sendNotification(payload.emitterId(), response);

    }

    // 알림 조회
    @Override
    public PagingResponse<NotificationResponse> searchNotifications(Pageable pageable, Sort sort, Long memberId) {
        Page<Notification> notifications = notificationQueryDslRepository.findNotifications(pageable, sort, memberId);
        Page<NotificationResponse> notificationResponse = notifications.map(NotificationResponse::from);

        return PagingResponse.from(notificationResponse);
    }

    // 알림 읽음 처리
    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationException::notfoundException);
        notification.markAsRead();
    }

    // 읽지 않은 알림 개수
    @Override
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    }

}
