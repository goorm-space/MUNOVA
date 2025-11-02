package com.space.munova.notification.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationResponse;
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
        // 연결 알림
        emitterService.sendNotification(emitter, memberId.toString(), CLIENT_CONNECT);
        log.info("SSE 구독 성공: emitterId={}", memberId);

        return emitter;
    }

    // 알림 발송
    @Override
    @Transactional
    public void sendNotification(NotificationPayload payload) {
        // 타입에 따라 DB 저장 여부 결정
        if (payload.type().isShouldSave()) {
            Notification notification = Notification.from(payload);
            notificationRepository.save(notification);
        }
        // 알림 전송
        emitterService.sendNotification(payload.emitterId(), payload.notificationData());
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
