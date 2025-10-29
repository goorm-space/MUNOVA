package com.space.munova.coupon.dto;

import com.space.munova.coupon.entity.CouponDetail;

import java.time.LocalDateTime;

public record SearchEventCouponResponse(
        Long couponDetailId,
        Long quantity,
        String couponName,
        DiscountPolicy discountPolicy,
        LocalDateTime expiredAt
) {
    public static SearchEventCouponResponse from(CouponDetail couponDetail) {
        return new SearchEventCouponResponse(
                couponDetail.getId(),
                couponDetail.getQuantity(),
                couponDetail.getCouponName(),
                couponDetail.getDiscountPolicy(),
                couponDetail.getExpiredAt()
        );
    }
}
