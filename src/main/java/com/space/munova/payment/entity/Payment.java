package com.space.munova.payment.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.order.entity.Order;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.exception.PaymentException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    private Long orderId;

    private String tossPaymentKey;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Long totalAmount;

    private Instant requestedAt;

    private Instant approvedAt;

    private String receipt;

    @Column(length = 64)
    private String lastTransactionKey;

    public static Payment create(Long orderId, TossPaymentResponse response) {
        Instant requestedAt = response.requestedAt().toInstant();
        Instant approvedAt = response.approvedAt() != null ?
                response.approvedAt().toInstant() : null;

        return Payment.builder()
                .orderId(orderId)
                .tossPaymentKey(response.paymentKey())
                .status(response.status())
                .method(response.method())
                .totalAmount(response.totalAmount())
                .requestedAt(requestedAt)
                .approvedAt(approvedAt)
                .receipt(response.receipt().url())
                .lastTransactionKey(response.lastTransactionKey())
                .build();
    }

    public void updatePaymentInfo(PaymentStatus status, String lastTransactionKey) {
        if (!this.status.isUpdatableStatus()) {
            throw PaymentException.illegalPaymentStateException(
                    String.format("현재 결제 상태: %s", this.status)
            );
        }

        this.status = status;
        this.lastTransactionKey = lastTransactionKey;
    }
}
