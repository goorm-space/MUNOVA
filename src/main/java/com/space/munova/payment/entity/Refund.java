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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(length = 64)
    private String transactionKey;

    private String cancelReason;

    private Long cancelAmount;

    private String cancelStatus;

    private ZonedDateTime canceledAt;
}
