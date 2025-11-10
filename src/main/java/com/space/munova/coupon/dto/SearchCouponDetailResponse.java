package com.space.munova.coupon.dto;

import com.space.munova.coupon.entity.CouponDetail;

import java.time.LocalDateTime;

public record SearchCouponDetailResponse(
        Long couponDetailId,
        Long quantity,
        Long remainQuantity,
        String couponName,
        DiscountPolicy discountPolicy,
        LocalDateTime publishAt,
        LocalDateTime expiredAt
) {

    public static SearchCouponDetailResponse from(CouponDetail couponDetail, Long remainQuantity) {
        return new SearchCouponDetailResponse(
                couponDetail.getId(),
                couponDetail.getQuantity(),
                remainQuantity,
                couponDetail.getCouponName(),
                couponDetail.getDiscountPolicy(),
                couponDetail.getPublishedAt(),
                couponDetail.getExpiredAt()
        );
    }
}
