package com.space.munova.coupon.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;
import com.space.munova.coupon.dto.SearchCouponDetailParams;
import com.space.munova.coupon.dto.SearchCouponDetailResponse;
import com.space.munova.coupon.service.CouponDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CouponDetailController {

    private final CouponDetailService couponDetailService;

    /**
     * 관리자 쿠폰조회
     */
    @GetMapping("/coupon")
    public ResponseApi<PagingResponse<SearchCouponDetailResponse>> searchAdminCoupon(
            @PageableDefault Pageable pageable,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Sort sort,
            @Valid SearchCouponDetailParams searchCouponDetailParams
    ) {
        PagingResponse<SearchCouponDetailResponse> couponDetail
                = couponDetailService.searchAdminCoupon(pageable, sort, searchCouponDetailParams);
        return ResponseApi.ok(couponDetail);
    }

    /**
     * 관리자 쿠폰등록
     */
    @PostMapping("/coupon")
    public ResponseApi<RegisterCouponDetailResponse> registerCoupon(
            @Valid @RequestBody RegisterCouponDetailRequest registerCouponDetailRequest
    ) {
        RegisterCouponDetailResponse registerCouponDetailResponse
                = couponDetailService.registerCoupon(registerCouponDetailRequest);
        return ResponseApi.ok(registerCouponDetailResponse);
    }

}
