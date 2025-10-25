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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private Long productId;

    private boolean clicked;
    private boolean liked;
    private boolean inCart;
    private boolean purchased;

    private LocalDateTime clickedAt;
    private LocalDateTime likedAt;
    private LocalDateTime incartAt;
    private LocalDateTime purchasedAt;

    private LocalDateTime lastUpdated;

    public UserActionSummary(Long memberId, Long productId, boolean clicked, boolean liked, boolean inCart, boolean purchased) {
        this.memberId = memberId;
        this.productId = productId;
        this.clicked = clicked;
        this.liked = liked;
        this.inCart = inCart;
        this.purchased = purchased;
        this.lastUpdated = LocalDateTime.now();
    }
}
