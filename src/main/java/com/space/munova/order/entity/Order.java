package com.space.munova.order.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Member member;

    @Column(nullable = false, unique = true)
    private String orderNum;

    private String userRequest;

    private Long originPrice;

    private Long couponId;

    private Integer discountPrice;

    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

    public void updateFinalOrder(Long originPrice, int discountPrice, Long totalPrice, Long couponId, OrderStatus status) {
        this.originPrice = originPrice;
        this.discountPrice = discountPrice;
        this.totalPrice = totalPrice;
        this.couponId = couponId;
        this.status = status;

        if (this.orderItems != null) {
            for (OrderItem orderItem : this.orderItems) {
                orderItem.updateStatus(status);
            }
        }
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;

        if (this.orderItems != null) {
            for (OrderItem orderItem : this.orderItems) {
                orderItem.updateStatus(status);
            }
        }
    }

    public static String generateOrderNum() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return (date + uuid).toUpperCase();
    }
}
