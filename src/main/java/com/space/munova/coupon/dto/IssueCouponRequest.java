package com.space.munova.coupon.dto;

import jakarta.validation.constraints.NotNull;

public record IssueCouponRequest(
        @NotNull(message = "쿠폰 아이디 필수")
        Long couponDetailId
) {
    public static IssueCouponRequest of(Long couponDetailId) {
        return new IssueCouponRequest(couponDetailId);
    }
}
