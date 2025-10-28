package com.space.munova.coupon.dto;

import com.space.munova.coupon.entity.CouponDetail;

import java.time.LocalDateTime;

public record SearchCouponDetailResponse(
        Long couponId,
        Long quantity,
        String couponName,
        DiscountPolicy discountPolicy,
        LocalDateTime publishAt,
        LocalDateTime expiredAt
) {

    public static SearchCouponDetailResponse from(CouponDetail couponDetail) {
        return new SearchCouponDetailResponse(
                couponDetail.getId(),
                couponDetail.getQuantity(),
                couponDetail.getCouponName(),
                couponDetail.getDiscountPolicy(),
                couponDetail.getPublishedAt(),
                couponDetail.getExpiredAt()
        );
    }
}
