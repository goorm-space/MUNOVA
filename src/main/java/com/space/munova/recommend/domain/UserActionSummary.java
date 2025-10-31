// 추천 전용 요약 테이블 -> redis 연동
package com.space.munova.recommend.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="user_action_summary", uniqueConstraints = @UniqueConstraint(columnNames={"memberId","productId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActionSummary {

    @Id
    @Column(name="user_action_summary_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private Long productId;

    private Integer clicked;
    private Boolean liked;
    private Boolean inCart;
    private Boolean purchased;

    private LocalDateTime clickedAt;
    private LocalDateTime likedAt;
    private LocalDateTime inCartAt;
    private LocalDateTime purchasedAt;

    private LocalDateTime lastUpdated;

    public UserActionSummary(Long memberId, Long productId, Integer clicked, Boolean liked, Boolean inCart, Boolean purchased) {
        this.memberId = memberId;
        this.productId = productId;
        this.clicked = clicked;
        this.liked = liked;
        this.inCart = inCart;
        this.purchased = purchased;
        this.lastUpdated = LocalDateTime.now();
    }
}
