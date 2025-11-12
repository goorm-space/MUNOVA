package com.space.munova.coupon.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.coupon.dto.CouponStatus;
import com.space.munova.coupon.dto.DiscountPolicy;
import com.space.munova.coupon.exception.CouponException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"memberId", "coupon_detail_id"})
})
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_detail_id")
    private CouponDetail couponDetail;

    private LocalDateTime usedAt;

    // 쿠폰 발급
    public static Coupon issuedCoupon(Long memberId, CouponDetail couponDetail) {
        return Coupon.builder()
                .memberId(memberId)
                .status(CouponStatus.ISSUED)
                .couponDetail(couponDetail)
                .build();
    }

    // 쿠폰 사용
    // 최종금액(원가 - 할인가) 반환
    public Long verifyCoupon(Long originalPrice) {
        // 쿠폰 검증
        validateCoupon();

        // 최종금액 계산
        return calculateFinalPrice(originalPrice);
    }

    // 쿠폰 사용
    public void updateCouponUsed() {
        status = CouponStatus.USED;
        usedAt = LocalDateTime.now();
    }

    // 할인금액 계산
    // 최종금액(원가 - 할인가) 반환
    private Long calculateFinalPrice(Long originalPrice) {
        DiscountPolicy discountPolicy = couponDetail.getDiscountPolicy();
        Long discountPrice = discountPolicy.calculateDiscountPrice(originalPrice);
        return Math.max(originalPrice - discountPrice, 0L);
    }

    // 쿠폰 검증
    private void validateCoupon() {
        // 발급 상태가 아닐 경우 사용 불가
        if (CouponStatus.USED.equals(status)) {
            throw CouponException.alreadyUsedException();
        }
        if (CouponStatus.EXPIRED.equals(status) || LocalDateTime.now().isAfter(couponDetail.getExpiredAt())) {
            throw CouponException.expiredException();
        }
    }

}
