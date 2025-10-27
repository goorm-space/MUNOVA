package com.space.munova.coupon.service;

import com.space.munova.coupon.dto.IssueCouponRequest;
import com.space.munova.coupon.dto.IssueCouponResponse;
import com.space.munova.coupon.entity.Coupon;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.exception.CouponException;
import com.space.munova.coupon.repository.CouponDetailRepository;
import com.space.munova.coupon.repository.CouponRedisRepository;
import com.space.munova.coupon.repository.CouponRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponDetailRepository couponDetailRepository;
    private final CouponRedisRepository couponRedisRepository;

    /**
     * 유저 쿠폰 발급
     */
    @Override
    @Transactional
    public IssueCouponResponse issueCoupon(IssueCouponRequest issueCouponRequest) {
        // 쿠폰 수량 확인
        Long couponDetailId = issueCouponRequest.couponDetailId();

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

        CouponDetail couponDetail = couponDetailRepository.findById(couponDetailId)
                .orElseThrow(CouponException::notFoundException);

        // 쿠폰 발급
        Long memberId = JwtHelper.getMemberId();
        Coupon coupon = Coupon.issuedCoupon(memberId, couponDetail);
        Coupon saveCoupon = couponRepository.save(coupon);

        return IssueCouponResponse.of(saveCoupon.getId(), saveCoupon.getStatus());
    }

}
