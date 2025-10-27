package com.space.munova.coupon.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.coupon.dto.IssueCouponRequest;
import com.space.munova.coupon.dto.IssueCouponResponse;
import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;
import com.space.munova.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 쿠폰 발급
     */
    @PostMapping("/{couponDetailId}")
    public ResponseApi<IssueCouponResponse> issueCoupon(@PathVariable Long couponDetailId) {
        IssueCouponRequest issueCouponRequest = IssueCouponRequest.of(couponDetailId);
        IssueCouponResponse issueCouponResponse = couponService.issueCoupon(issueCouponRequest);

        return ResponseApi.ok(issueCouponResponse);
    }

    /**
     * 쿠폰 사용
     */
    @PatchMapping("/{couponId}")
    public ResponseApi<UseCouponResponse> useCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody UseCouponRequest useCouponRequest
    ) {
        UseCouponResponse useCouponResponse = couponService.useCoupon(couponId, useCouponRequest);

        return ResponseApi.ok(useCouponResponse);
    }

}
