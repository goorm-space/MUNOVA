package com.space.munova.payment.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.payment.dto.CancelDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long id;

    private Long paymentId;

    private Long orderItemId;

    private String paymentKey;

    @Column(length = 64)
    private String transactionKey;

    @Column(nullable = false)
    private String cancelReason;

    @Column(nullable = false)
    private Long cancelAmount;

    private Instant canceledAt;

    public static Refund create(Long paymentId, Long orderItemId, String paymentKey, CancelDto cancel) {
        return Refund.builder()
                .paymentId(paymentId)
                .orderItemId(orderItemId)
                .paymentKey(paymentKey)
                .transactionKey(cancel.transactionKey())
                .cancelReason(cancel.cancelReason())
                .cancelAmount(cancel.cancelAmount())
                .canceledAt(cancel.canceledAt().toInstant())
                .build();
    }

    public static Refund createWhenRollBack(String paymentKey, CancelDto cancel) {
        return Refund.builder()
                .paymentKey(paymentKey)
                .transactionKey(cancel.transactionKey())
                .cancelReason(cancel.cancelReason())
                .cancelAmount(cancel.cancelAmount())
                .canceledAt(cancel.canceledAt().toInstant())
                .build();
    }
}
