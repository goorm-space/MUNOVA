package com.space.munova.payment.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.payment.dto.CancelReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends BaseEntity {

    public enum RefundStatus {
        DONE
    }

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

    private String cancelStatus;

    private ZonedDateTime canceledAt;
}
