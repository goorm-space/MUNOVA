package com.space.munova.coupon.dto;

public record IssueCouponResponse(
        Long couponId,
        CouponStatus status
) {

    public static IssueCouponResponse of(Long couponId, CouponStatus status) {
        return new IssueCouponResponse(couponId, status);
    }

}
