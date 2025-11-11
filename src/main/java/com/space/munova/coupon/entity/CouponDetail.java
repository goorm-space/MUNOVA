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

    // 남은 수량
    private Long remainQuantity;

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
                .remainQuantity(request.quantity())
                .couponName(request.couponName())
                .discountPolicy(discountPolicy)
                .publisherId(publisherId)
                .publishedAt(LocalDateTime.now())
                .expiredAt(request.expiredAt())
                .build();
    }

    // 쿠폰 발행일자 검증
    public void validatePublished() {
        if (LocalDateTime.now().isBefore(publishedAt)) {
            throw CouponException.notPublishedException();
        }
    }

    // 쿠폰 재고 차감
    public void decreaseRemainQuantity() {
        if (remainQuantity <= 0) {
            throw CouponException.soldOutException();
        }
        remainQuantity -= 1;
    }

}
