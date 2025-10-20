package com.space.munova.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "createdAt", updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updatedAt")
    protected LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = null;
    }
}
