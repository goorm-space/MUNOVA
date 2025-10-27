package com.space.munova.coupon.dto;

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

}
