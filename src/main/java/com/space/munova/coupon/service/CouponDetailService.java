package com.space.munova.coupon.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;
import com.space.munova.coupon.dto.SearchCouponDetailParams;
import com.space.munova.coupon.dto.SearchCouponDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface CouponDetailService {

    /**
     * 관리자 쿠폰조회
     */
    PagingResponse<SearchCouponDetailResponse> searchAdminCoupon(Pageable pageable, Sort sort, SearchCouponDetailParams searchCouponDetailParams);

    /**
     * 관리자 쿠폰등록
     */
    RegisterCouponDetailResponse registerCoupon(RegisterCouponDetailRequest registerCouponDetailRequest);
}
