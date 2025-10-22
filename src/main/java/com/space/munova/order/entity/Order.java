package com.space.munova.order.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Member member;

    @Column(nullable = false, unique = true)
    private String orderNum;

    private String userRequest;

    @Column(nullable = false)
    private Long originPrice;

    private Long couponId;

    @Column(nullable = false)
    private Integer discountPrice;

    @Column(nullable = false)
    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    public void setPrices(Long originPrice, int discountPrice, Long totalPrice) {
        this.originPrice = originPrice;
        this.discountPrice = discountPrice;
        this.totalPrice = totalPrice;
    }

    public static String generateOrderNum() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return (date + uuid).toUpperCase();
    }
}
