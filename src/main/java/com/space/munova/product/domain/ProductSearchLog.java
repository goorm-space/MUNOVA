package com.space.munova.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;


@Entity
@Table(name = "product_search_log",
        indexes = {
                @Index(name = "idx_user_search_member_id", columnList = "member_id"),
                @Index(name = "idx_user_search_category_id", columnList = "search_category_id"),
                @Index(name = "idx_user_search_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_search_log_id")
    private Long id;

    private Long memberId;

    private String searchDetail;

    private Long searchCategoryId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}