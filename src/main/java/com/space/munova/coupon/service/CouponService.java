package com.space.munova.coupon.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface CouponService {

    // 쿠폰 목록
    PagingResponse<SearchCouponResponse> searchCoupons(Pageable pageable, Sort sort, SearchCouponParams params);

    // 쿠폰 발급
    IssueCouponResponse issueCoupon(IssueCouponRequest issueCouponRequest);

    // 쿠폰 확인
    UseCouponResponse calculateAmountWithCoupon(Long couponId, UseCouponRequest useCouponRequest);

    // 쿠폰 사용
    void useCoupon(Long couponId);
}
