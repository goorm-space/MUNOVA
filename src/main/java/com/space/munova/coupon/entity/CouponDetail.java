package com.space.munova.coupon.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.coupon.dto.CouponType;
import com.space.munova.coupon.dto.DiscountPolicy;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.exception.CouponException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_detail_id")
    private Long id;

    // 발행 수량
    private Long quantity;

    // 쿠폰명
    private String couponName;

    // 할인 정책
    @Embedded
    private DiscountPolicy discountPolicy;

    // 발행 담당 관리자
    private Long publisherId;

    // 발행일자
    private LocalDateTime publishedAt;

    // 만료일자
    private LocalDateTime expiredAt;

    // 할인금액 계산
    // 최종금액(원가 - 할인가) 반환
    public Long calculateFinalPrice(Long originalPrice) {
        validateDiscount(originalPrice);
        CouponType couponType = discountPolicy.getCouponType();
        Long maxDiscountAmount = discountPolicy.getMaxDiscountAmount();
        Long priceAfterDiscount = couponType.calculate(originalPrice, discountPolicy.getDiscountAmount());

        if (maxDiscountAmount <= 0) {
            // 최대 결제금액 제한이 없을때 할인 금액 적용
            return priceAfterDiscount;
        }

        long calculateDiscount = originalPrice - priceAfterDiscount;
        long actualDiscount = Math.min(calculateDiscount, maxDiscountAmount);
        return originalPrice - actualDiscount;
    }

    // RegisterCouponDetailRequest DTO -> Entity 변환
    public static CouponDetail of(RegisterCouponDetailRequest request, Long publisherId) {
        DiscountPolicy discountPolicy = DiscountPolicy.builder()
                .couponType(CouponType.valueOf(request.couponType()))
                .discountAmount(request.discountAmount())
                .maxDiscountAmount(request.maxDiscountAmount() != null ? request.maxDiscountAmount() : 0L)
                .minPaymentAmount(request.minPaymentAmount() != null ? request.minPaymentAmount() : 0L)
                .build();

        return CouponDetail.builder()
                .quantity(request.quantity())
                .couponName(request.couponName())
                .discountPolicy(discountPolicy)
                .publisherId(publisherId)
                .publishedAt(LocalDateTime.now())
                .expiredAt(request.expiredAt())
                .build();
    }

    // 할인 가능여부
    private void validateDiscount(Long originalPrice) {
        // 만료일자 확인
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiredAt)) {
            throw CouponException.expiredException();
        }
        // 최소금액 확인
        if (discountPolicy.getMinPaymentAmount() > originalPrice) {
            throw CouponException.invalidMinPaymentException();
        }
    }

}
