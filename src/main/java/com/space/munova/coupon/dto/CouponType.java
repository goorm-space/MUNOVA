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

    public Long calculateDiscountAmount(Long originalPrice, Long discountAmount, Long maxDiscountAmount) {
        // 할인 금액 계산
        Long discountPrice = calculator.calculate(originalPrice, discountAmount);
        // 최대 할인 금액 제한
        if (maxDiscountAmount > 0) {
            discountPrice = Math.min(discountPrice, maxDiscountAmount);
        }
        return discountPrice;
    }

    // 퍼센트 할인 계산
    // - 할인 금액 리턴
    private static Long calculatePercent(Long originalPrice, Long discountAmount) {
        BigDecimal originPrice = BigDecimal.valueOf(originalPrice);
        BigDecimal discountRate = BigDecimal.valueOf(discountAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        // 할인금액 계산 (원가 * 할인율), 소수점 버림
        BigDecimal discountPrice = originPrice.multiply(discountRate).setScale(0, RoundingMode.FLOOR);
        return discountPrice.longValue();
    }

    // 정액 할인 계산
    // - 할인 금액 리턴
    private static Long calculateFixed(Long originalPrice, Long discountAmount) {
        return discountAmount;
    }

    @FunctionalInterface
    private interface DiscountCalculator {
        Long calculate(Long originalPrice, Long discountAmount);
    }
}
