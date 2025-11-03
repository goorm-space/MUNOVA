package com.space.munova.notification.repository;

import com.space.munova.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 미읽음 알림 개수 조회
    long countByMemberIdAndIsReadFalse(Long memberId);
}
