package com.space.munova.coupon.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface CouponDetailService {

    /**
     * 선착순 쿠폰조회
     */
    PagingResponse<SearchEventCouponResponse> searchEventCoupon(Pageable pageable, Sort sort, Long memberId);

    /**
     * 관리자 쿠폰조회
     */
    PagingResponse<SearchCouponDetailResponse> searchAdminCoupon(
            Pageable pageable, Sort sort, SearchCouponDetailParams searchCouponDetailParams
    );

    /**
     * 관리자 쿠폰등록
     */
    RegisterCouponDetailResponse registerCoupon(Long memberId, RegisterCouponDetailRequest registerCouponDetailRequest);
}
