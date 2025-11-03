package com.space.munova.notification.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.notification.common.NotificationMessage;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private String redirectUrl;

    @Builder.Default
    private Boolean isRead = false;

    // 알림 읽음 상태 변경
    public void markAsRead() {
        this.isRead = true;
    }

    public static Notification from(NotificationPayload payload) {
        NotificationMessage notificationMessage = payload.notificationData();
        return Notification.builder()
                .memberId(payload.memberId())
                .type(payload.type())
                .title(notificationMessage.getTitle())
                .content(payload.message())
                .redirectUrl(notificationMessage.getRedirectUrl())
                .build();
    }
}
