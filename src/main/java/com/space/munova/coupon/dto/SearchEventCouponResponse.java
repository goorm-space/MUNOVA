package com.space.munova.coupon.dto;

import java.time.LocalDateTime;

public record SearchEventCouponResponse(
        Long couponDetailId,
        Long quantity,
        Long remainQuantity,
        String couponName,
        DiscountPolicy discountPolicy,
        Boolean isAlreadyIssued,
        LocalDateTime expiredAt
) {
    public static SearchEventCouponResponse from(CouponDetailWithIssueStatus couponDetail, Long remainQuantity) {
        return new SearchEventCouponResponse(
                couponDetail.couponDetailId(),
                couponDetail.quantity(),
                remainQuantity,
                couponDetail.couponName(),
                couponDetail.discountPolicy(),
                couponDetail.isAlreadyIssued(),
                couponDetail.expiredAt()
        );
    }
}
