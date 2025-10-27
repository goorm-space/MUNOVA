package com.space.munova.coupon.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.coupon.dto.CouponStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static Coupon issuedCoupon(Long memberId, CouponDetail couponDetail) {
        return Coupon.builder()
                .memberId(memberId)
                .status(CouponStatus.ISSUED)
                .couponDetail(couponDetail)
                .build();
    }

}
