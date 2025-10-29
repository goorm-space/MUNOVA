package com.space.munova.coupon.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.core.utils.TimeHelper;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;
import com.space.munova.coupon.dto.SearchCouponDetailParams;
import com.space.munova.coupon.dto.SearchCouponDetailResponse;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.repository.CouponDetailRepository;
import com.space.munova.coupon.repository.CouponDetailSearchQueryDslRepository;
import com.space.munova.coupon.repository.CouponRedisRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponDetailServiceImpl implements CouponDetailService {

    private final CouponRedisRepository couponRedisRepository;
    private final CouponDetailRepository couponDetailRepository;
    private final CouponDetailSearchQueryDslRepository couponDetailSearchQueryDslRepository;

    /**
     * 관리자 쿠폰조회
     */
    @Override
    public PagingResponse<SearchCouponDetailResponse> searchAdminCoupon(
            Pageable pageable, Sort sort, SearchCouponDetailParams searchCouponDetailParams
    ) {
        Page<CouponDetail> couponDetail
                = couponDetailSearchQueryDslRepository.findByCouponDetailParams(pageable, sort, searchCouponDetailParams);
        Page<SearchCouponDetailResponse> couponDetailResponses = couponDetail.map(SearchCouponDetailResponse::from);

        return PagingResponse.from(couponDetailResponses);
    }

    /**
     * 관리자 쿠폰등록
     */
    @Override
    @Transactional
    public RegisterCouponDetailResponse registerCoupon(RegisterCouponDetailRequest registerCouponDetailRequest) {
        Long memberId = JwtHelper.getMemberId();
        CouponDetail couponDetail = CouponDetail.of(registerCouponDetailRequest, memberId);
        CouponDetail saveCoupon = couponDetailRepository.save(couponDetail);

        // 쿠폰 수량 저장
        Long expiredAt = TimeHelper.toEpochMilli(saveCoupon.getExpiredAt());
        couponRedisRepository.saveQuantity(saveCoupon.getId(), saveCoupon.getQuantity(), expiredAt);

        return RegisterCouponDetailResponse.of(saveCoupon.getId());
    }
}
