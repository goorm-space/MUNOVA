package com.space.munova.coupon.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.coupon.dto.CouponStatus;
import com.space.munova.coupon.dto.CouponType;
import com.space.munova.coupon.dto.DiscountPolicy;
import com.space.munova.coupon.exception.CouponException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"memberId", "coupon_detail_id"})
})
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_detail_id")
    private CouponDetail couponDetail;

    private LocalDateTime usedAt;

    // 쿠폰 발급
    public static Coupon issuedCoupon(Long memberId, CouponDetail couponDetail) {
        return Coupon.builder()
                .memberId(memberId)
                .status(CouponStatus.ISSUED)
                .couponDetail(couponDetail)
                .build();
    }

    // 쿠폰 사용
    // 최종금액(원가 - 할인가) 반환
    public Long useCoupon(Long originalPrice) {
        // 쿠폰 검증
        validateCoupon(originalPrice);

        // 최종금액 계산
        Long finalPrice = calculateFinalPrice(originalPrice);

        // 상태변경
        status = CouponStatus.USED;
        usedAt = LocalDateTime.now();

        return finalPrice;
    }

    // 할인금액 계산
    // 최종금액(원가 - 할인가) 반환
    private Long calculateFinalPrice(Long originalPrice) {
        DiscountPolicy discountPolicy = couponDetail.getDiscountPolicy();
        CouponType couponType = discountPolicy.getCouponType();
        Long maxDiscountAmount = discountPolicy.getMaxDiscountAmount();
        Long priceAfterDiscount = couponType.calculateDiscountAmount(originalPrice, discountPolicy.getDiscountAmount());

        if (maxDiscountAmount <= 0) {
            // 최대 할인금액 제한이 없을때 할인 금액 적용
            return priceAfterDiscount;
        }

        // 최대 할인금액에 따라 계산
        long calculateDiscount = originalPrice - priceAfterDiscount;
        long actualDiscount = Math.min(calculateDiscount, maxDiscountAmount);
        return originalPrice - actualDiscount;
    }

    // 쿠폰 검증
    private void validateCoupon(Long originalPrice) {
        // 발급 상태가 아닐 경우 사용 불가
        if (status.equals(CouponStatus.USED)) {
            throw CouponException.alreadyUsedException();
        } else if (status.equals(CouponStatus.EXPIRED) || LocalDateTime.now().isAfter(couponDetail.getExpiredAt())) {
            throw CouponException.expiredException();
        }
        // 최소금액 확인
        if (couponDetail.getDiscountPolicy().getMinPaymentAmount() > originalPrice) {
            throw CouponException.invalidMinPaymentException();
        }
    }

}
