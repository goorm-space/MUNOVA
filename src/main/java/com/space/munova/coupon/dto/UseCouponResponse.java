package com.space.munova.coupon.dto;

public record UseCouponResponse(
        Long originalPrice, // 원가
        Long discountPrice, // 쿠폰 할인가격
        Long finalPrice     // 쿠폰 적용가격
) {
    public static UseCouponResponse of(Long originalPrice, Long discountPrice, Long finalPrice) {
        return new UseCouponResponse(originalPrice, discountPrice, finalPrice);
    }
}
