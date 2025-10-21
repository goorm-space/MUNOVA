package com.space.munova.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    //    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    //    @JoinColumn(name = "product_detail_id")
    private Long productDetailId;

    private String productName;

    private Long originPrice;

    private Integer quantity;

    private Long couponId;

    private Integer discountPrice;

    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;
}
