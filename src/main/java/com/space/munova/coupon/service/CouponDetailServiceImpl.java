package com.space.munova.coupon.service;

import com.space.munova.core.utils.TimeHelper;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.repository.CouponDetailRepository;
import com.space.munova.coupon.repository.CouponRedisRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponDetailServiceImpl implements CouponDetailService {

    private final CouponDetailRepository couponDetailRepository;
    private final CouponRedisRepository couponRedisRepository;

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
