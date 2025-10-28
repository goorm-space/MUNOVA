package com.space.munova.coupon.dto;

import com.space.munova.coupon.entity.Coupon;
import com.space.munova.coupon.entity.CouponDetail;

import java.time.LocalDateTime;

public record SearchCouponResponse(
        Long id,
        CouponStatus status,
        String couponName,
        CouponType couponType,
        Long discountAmount,
        Long maxDiscountAmount,
        Long minPaymentAmount,
        LocalDateTime publishedAt,
        LocalDateTime expiredAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {
    public static SearchCouponResponse from(Coupon coupon) {
        CouponDetail couponDetail = coupon.getCouponDetail();
        DiscountPolicy discountPolicy = couponDetail.getDiscountPolicy();

        return new SearchCouponResponse(
                coupon.getId(),
                coupon.getStatus(),
                couponDetail.getCouponName(),
                discountPolicy.getCouponType(),
                discountPolicy.getDiscountAmount(),
                discountPolicy.getMaxDiscountAmount(),
                discountPolicy.getMinPaymentAmount(),
                couponDetail.getPublishedAt(),
                couponDetail.getExpiredAt(),
                coupon.getUsedAt(),
                coupon.getCreatedAt()
        );
    }
}
