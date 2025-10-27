package com.space.munova.coupon.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
public enum CouponType {

    PERCENT(CouponType::calculatePercent, "퍼센트 할인"),
    FIXED(CouponType::calculateFixed, "정액 할인");

    private final DiscountCalculator calculator;
    private final String description;

    public Long calculate(Long originalPrice, Long discountAmount) {
        return calculator.calculate(originalPrice, discountAmount);
    }

    // 퍼센트 할인 계산
    private static Long calculatePercent(Long originalPrice, Long discountAmount) {
        BigDecimal originPrice = BigDecimal.valueOf(originalPrice);
        BigDecimal discountRate = BigDecimal.valueOf(discountAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        // 할인금액 계산 (원가 * 할인율), 소수점 버림
        BigDecimal discountPrice = originPrice.multiply(discountRate).setScale(0, RoundingMode.FLOOR);
        // 최종 금액
        return Math.max(originPrice.subtract(discountPrice).longValue(), 0L);
    }

    // 정액 할인 계산
    private static Long calculateFixed(Long originalPrice, Long discountAmount) {
        return Math.max(originalPrice - discountAmount, 0L);
    }

    @FunctionalInterface
    private interface DiscountCalculator {
        Long calculate(Long originalPrice, Long discountAmount);
    }
}
