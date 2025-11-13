package com.space.munova.coupon.dto;

import jakarta.validation.constraints.NotNull;

public record IssueCouponRequest(
        @NotNull(message = "쿠폰 아이디 필수")
        Long couponDetailId,
        @NotNull(message = "유저 아이디 필수")
        Long memberId
) {
    public static IssueCouponRequest of(Long couponDetailId, Long memberId) {
        return new IssueCouponRequest(couponDetailId, memberId);
    }
}
