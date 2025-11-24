package com.space.munova.coupon.service;

import com.space.munova.core.annotation.VelvetQ;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.*;
import com.space.munova.coupon.entity.Coupon;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.exception.CouponException;
import com.space.munova.coupon.repository.CouponDetailRepository;
import com.space.munova.coupon.repository.CouponRepository;
import com.space.munova.coupon.repository.CouponSearchQueryDslRepository;
import com.space.munova.velvetQ.dto.VelvetQDomainType;
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
    @VelvetQ(domain = VelvetQDomainType.COUPON, resourceId = "#issueCouponRequest.couponDetailId")
    public IssueCouponResponse issueCoupon(IssueCouponRequest issueCouponRequest) {
        Long memberId = issueCouponRequest.memberId();
        Long couponDetailId = issueCouponRequest.couponDetailId();

        // 쿠폰 발급 중복체크
        Long duplicateCount = couponRepository.findDuplicateIssuedCouponWithLock(couponDetailId, memberId);
        if (duplicateCount > 0) {
            throw CouponException.duplicateIssueException();
        }

        CouponDetail couponDetail = couponDetailRepository.findByIdWithLock(couponDetailId)
                .orElseThrow(CouponException::notFoundException);

        // 발행일자 이전에 발급 요청시 예외
        couponDetail.validatePublished();

        // 쿠폰 발급
        Coupon coupon = Coupon.issuedCoupon(memberId, couponDetail);
        Coupon savedCoupon = couponRepository.save(coupon);

        // 쿠폰 재고 차감
        couponDetail.decreaseRemainQuantity();

        return IssueCouponResponse.of(savedCoupon.getId(), savedCoupon.getStatus());
    }

    /**
     * 쿠폰 확인
     */
    @Override
    @Transactional
    public UseCouponResponse calculateAmountWithCoupon(Long couponId, UseCouponRequest useCouponRequest) {
        Coupon coupon = couponRepository.findWithCouponDetailById(couponId)
                .orElseThrow(CouponException::notFoundException);

        Long originalPrice = useCouponRequest.originalPrice();
        Long finalPrice = coupon.verifyCoupon(originalPrice);

        return UseCouponResponse.of(originalPrice, originalPrice - finalPrice, finalPrice);
    }

    /**
     * 쿠폰 사용
     */
    @Override
    @Transactional
    public void useCoupon(Long couponId) {
        Coupon coupon = couponRepository.findWithCouponDetailById(couponId)
                .orElseThrow(CouponException::notFoundException);

        coupon.updateCouponUsed();
    }

}
