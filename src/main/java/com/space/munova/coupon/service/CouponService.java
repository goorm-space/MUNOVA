package com.space.munova.coupon.service;

import com.space.munova.coupon.dto.IssueCouponRequest;
import com.space.munova.coupon.dto.IssueCouponResponse;

public interface CouponService {

    /**
     * 유저 쿠폰 발급
     */
    IssueCouponResponse issueCoupon(IssueCouponRequest issueCouponRequest);
}
