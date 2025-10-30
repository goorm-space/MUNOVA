package com.space.munova.coupon.dto;

import com.space.munova.coupon.exception.CouponException;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Builder
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DiscountPolicy {

    // 쿠폰타입 (PERCENT, FIXED)
    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    // 할인율 또는 할인금액
    // CouponType PERCENT: 5(%)
    // CouponType FIXED: 5000(원)
    private Long discountAmount;

    // 최대 할인 금액
    @Builder.Default
    @ColumnDefault("0")
    private Long maxDiscountAmount = 0L;

    // 최소 결제 금액
    @Builder.Default
    @ColumnDefault("0")
    private Long minPaymentAmount = 0L;

    // 할인가격 계산
    public Long calculateDiscountPrice(Long originalPrice) {
        // 검증
        validateDiscountPolicy(originalPrice);
        // 할인가 계산
        return couponType.calculateDiscountAmount(originalPrice, discountAmount, maxDiscountAmount);
    }

    // 할인정책 검증
    private void validateDiscountPolicy(Long originalPrice) {
        // 최소금액 확인
        if (minPaymentAmount > originalPrice) {
            throw CouponException.invalidMinPaymentException();
        }
    }
}
