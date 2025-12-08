package com.space.munova.product.infra.mysql;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.product.domain.enums.EventType;
import com.space.munova.product.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "product_outbox")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_outbox_id")
    private Long id;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "event_value", columnDefinition = "TEXT", nullable = false)
    private String eventValue;  //  JSON 문자열로 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;


    public static ProductOutbox from(EventType eventType, String jsonEventValue) {
        return ProductOutbox.builder()
                .eventType(eventType)
                .eventValue(jsonEventValue)
                .status(OutboxStatus.PENDING)
                .build();
    }

    public void changePublishStatus() {
        this.status = OutboxStatus.PUBLISHED;
    }

    public void changeFailedStatus() {
        this.status = OutboxStatus.FAILED;
    }
}