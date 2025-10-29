package com.space.munova.coupon.dto;

import com.space.munova.coupon.entity.CouponDetail;

import java.time.LocalDateTime;

public record CouponDetailWithIssueStatus(
        Long couponDetailId,
        String couponName,
        Long quantity,
        DiscountPolicy discountPolicy,
        LocalDateTime publishedAt,
        LocalDateTime expiredAt,
        Boolean isAlreadyIssued
) {
    public static CouponDetailWithIssueStatus from(CouponDetail couponDetail, Boolean isAlreadyIssued) {
        DiscountPolicy discountPolicy = couponDetail.getDiscountPolicy();

        return new CouponDetailWithIssueStatus(
                couponDetail.getId(),
                couponDetail.getCouponName(),
                couponDetail.getQuantity(),
                discountPolicy,
                couponDetail.getPublishedAt(),
                couponDetail.getExpiredAt(),
                isAlreadyIssued
        );
    }
}