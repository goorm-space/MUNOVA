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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String tossPaymentKey;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Long totalAmount;

    private ZonedDateTime requestedAt;

    private ZonedDateTime approvedAt;

    private String receipt;

    @Column(length = 64)
    private String lastTransactionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private String paymentObject;

    public void updatePaymentInfo(TossPaymentResponse response) {
        if (this.status != PaymentStatus.DONE && this.status != PaymentStatus.PARTIAL_CANCELED) {
            throw PaymentException.illegalPaymentStateException(
                    String.format("현재 결제 상태: %s", this.status)
            );
        }

        this.status = response.status();
        this.lastTransactionKey = response.lastTransactionKey();
        this.paymentObject = response.toString();
    }
}
