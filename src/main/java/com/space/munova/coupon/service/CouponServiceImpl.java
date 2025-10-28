package com.space.munova.coupon.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.*;
import com.space.munova.coupon.entity.Coupon;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.exception.CouponException;
import com.space.munova.coupon.repository.CouponDetailRepository;
import com.space.munova.coupon.repository.CouponRedisRepository;
import com.space.munova.coupon.repository.CouponRepository;
import com.space.munova.coupon.repository.CouponSearchQueryDslRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponDetailRepository couponDetailRepository;
    private final CouponRedisRepository couponRedisRepository;
    private final CouponSearchQueryDslRepository couponSearchQueryDslRepository;

    /**
     * 쿠폰 목록 조회
     */
    @Override
    public PagingResponse<SearchCouponResponse> searchCoupons(Pageable pageable, Sort sort, SearchCouponParams params) {
        Page<Coupon> coupons = couponSearchQueryDslRepository.findByCouponParams(pageable, sort, params);
        Page<SearchCouponResponse> couponsResponse = coupons.map(SearchCouponResponse::from);

        return PagingResponse.from(couponsResponse);
    }

    /**
     * 쿠폰 발급
     */
    @Override
    @Transactional
    public IssueCouponResponse issueCoupon(IssueCouponRequest issueCouponRequest) {
        // 쿠폰 수량 확인
        Long couponDetailId = issueCouponRequest.couponDetailId();
        Long memberId = JwtHelper.getMemberId();

        // 쿠폰 중복 발급 확인
        if (couponRepository.existsByMemberIdAndCouponDetailId(memberId, couponDetailId)) {
            throw CouponException.duplicateIssueException();
        }

        CouponDetail couponDetail = couponDetailRepository.findById(couponDetailId)
                .orElseThrow(CouponException::notFoundException);

        // 발행일자 이전에 발급 요청시 예외
        couponDetail.validatePublished();

        // 쿠폰 재고 차감
        Long couponRemain = couponRedisRepository.decreaseQuantity(couponDetailId);
        if (couponRemain == null) {
            throw CouponException.notFoundException();
        }
        if (couponRemain < 0) {
            // 재고 소진
            couponRedisRepository.delete(couponDetailId);
            throw CouponException.soldOutException();
        }

        // 쿠폰 발급
        Coupon coupon = Coupon.issuedCoupon(memberId, couponDetail);
        Coupon saveCoupon = couponRepository.save(coupon);

        return IssueCouponResponse.of(saveCoupon.getId(), saveCoupon.getStatus());
    }

    /**
     * 쿠폰 사용
     */
    @Override
    @Transactional
    public UseCouponResponse useCoupon(Long couponId, UseCouponRequest useCouponRequest) {
        Coupon coupon = couponRepository.findWithCouponDetailById(couponId)
                .orElseThrow(CouponException::notFoundException);

        // 쿠폰 사용
        Long originalPrice = useCouponRequest.originalPrice();
        Long finalPrice = coupon.useCoupon(originalPrice);

        return UseCouponResponse.of(originalPrice, originalPrice - finalPrice, finalPrice);
    }


}
