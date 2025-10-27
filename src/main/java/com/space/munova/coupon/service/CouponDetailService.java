package com.space.munova.coupon.service;

import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;

public interface CouponDetailService {

    /**
     * 관리자 쿠폰등록
     */
    RegisterCouponDetailResponse registerCoupon(RegisterCouponDetailRequest registerCouponDetailRequest);
}
