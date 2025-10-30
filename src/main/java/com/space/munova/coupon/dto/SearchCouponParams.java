package com.space.munova.coupon.dto;

import com.space.munova.core.utils.ValidEnum;

public record SearchCouponParams(
        @ValidEnum(enumClass = CouponStatus.class)
        String status
) {
}
