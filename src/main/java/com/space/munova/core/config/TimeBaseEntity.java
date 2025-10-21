package com.space.munova.core.config;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/// 해당 클래스 상속 받을경우
/// 상속받은 엔터티 테이블은 createAt updatedAt이 생기고 자동으로 업데이트된다
/// 메인 클래스 상단에 @EnableJpaAuditing 추가
@EntityListeners(AuditingEntityListener.class)
///  공통 매핑 정보가 필요할때 속성만 받아 사용할 수 있게하는 어노테이션
@MappedSuperclass
@Getter
public class TimeBaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}
