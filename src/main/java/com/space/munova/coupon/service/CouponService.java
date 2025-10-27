package com.space.munova.coupon.service;

import com.space.munova.coupon.dto.IssueCouponRequest;
import com.space.munova.coupon.dto.IssueCouponResponse;
import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;

public interface CouponService {

    /**
     * 쿠폰 발급
     */
    IssueCouponResponse issueCoupon(IssueCouponRequest issueCouponRequest);

    /**
     * 쿠폰 사용
     */
    UseCouponResponse useCoupon(Long couponId, UseCouponRequest useCouponRequest);
}
