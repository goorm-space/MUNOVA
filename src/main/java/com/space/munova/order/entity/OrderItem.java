package com.space.munova.order.entity;

import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.product.domain.ProductDetail;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_detail_id")
    private ProductDetail productDetail;

    @Column(name = "product_name", nullable = false)
    private String nameSnapshot;

    @Column(name = "price", nullable = false)
    private Long priceSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public static OrderItem create(Order order, ProductDetail productDetail, int quantity) {
        return OrderItem.builder()
                .order(order)
                .productDetail(productDetail)
                .nameSnapshot(productDetail.getNameSnapshot())
                .priceSnapshot(productDetail.getPriceSnapshot())
                .quantity(quantity)
                .status(OrderStatus.CREATED)
                .build();
    }

    public long calculateAmount() {
        return this.priceSnapshot * this.quantity;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }
}
