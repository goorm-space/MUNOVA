package com.space.munova.coupon.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.*;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 쿠폰 목록 조회
     */
    @GetMapping
    public ResponseApi<PagingResponse<SearchCouponResponse>> searchCoupons(
            @PageableDefault Pageable pageable,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Sort sort,
            @Valid SearchCouponParams searchCouponParams
    ) {
        PagingResponse<SearchCouponResponse> coupons = couponService.searchCoupons(pageable, sort, searchCouponParams);
        return ResponseApi.ok(coupons);
    }

    /**
     * 쿠폰 발급
     */
    @PostMapping("/{couponDetailId}")
    public ResponseApi<IssueCouponResponse> issueCoupon(@PathVariable Long couponDetailId) {
        Long memberId = JwtHelper.getMemberId();
        IssueCouponRequest issueCouponRequest = IssueCouponRequest.of(couponDetailId, memberId);
        IssueCouponResponse issueCouponResponse = couponService.issueCoupon(issueCouponRequest);
        return ResponseApi.ok(issueCouponResponse);
    }

    /**
     * 쿠폰 사용
     */
    @PatchMapping("/{couponId}")
    public ResponseApi<UseCouponResponse> verifyCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody UseCouponRequest useCouponRequest
    ) {
        UseCouponResponse useCouponResponse = couponService.calculateAmountWithCoupon(couponId, useCouponRequest);
        return ResponseApi.ok(useCouponResponse);
    }
}
