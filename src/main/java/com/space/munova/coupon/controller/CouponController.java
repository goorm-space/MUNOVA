package com.space.munova.coupon.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.coupon.dto.IssueCouponRequest;
import com.space.munova.coupon.dto.IssueCouponResponse;
import com.space.munova.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 유저 쿠폰 발급
     */
    @PostMapping("/coupon/{couponDetailId}")
    public ResponseApi<IssueCouponResponse> issueCoupon(@PathVariable Long couponDetailId) {
        IssueCouponRequest issueCouponRequest = IssueCouponRequest.of(couponDetailId);
        IssueCouponResponse issueCouponResponse = couponService.issueCoupon(issueCouponRequest);

        return ResponseApi.ok(issueCouponResponse);
    }
}
