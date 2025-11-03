package com.space.munova.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_click_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductClickLog {

    @Id
    @Column(name="product_click_log_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productClickLogId;

    private Long memberId;

    private Long productId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}