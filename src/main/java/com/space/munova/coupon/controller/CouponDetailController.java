package com.space.munova.coupon.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;
import com.space.munova.coupon.service.CouponDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponDetailController {

    private final CouponDetailService couponDetailService;

    /**
     * 관리자 쿠폰등록
     */
    @PostMapping("/admin/coupon")
    public ResponseApi<RegisterCouponDetailResponse> registerCoupon(
            @Valid @RequestBody RegisterCouponDetailRequest registerCouponDetailRequest
    ) {
        RegisterCouponDetailResponse registerCouponDetailResponse
                = couponDetailService.registerCoupon(registerCouponDetailRequest);
        return ResponseApi.ok(registerCouponDetailResponse);
    }


}
