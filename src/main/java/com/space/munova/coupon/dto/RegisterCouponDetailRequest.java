package com.space.munova.coupon.dto;

import com.space.munova.core.utils.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RegisterCouponDetailRequest(
        @NotNull(message = "쿠폰 수량 필수")
        Long quantity,

        @NotBlank(message = "쿠폰명 필수")
        String couponName,

        @NotBlank(message = "쿠폰타입 필수")
        @ValidEnum(enumClass = CouponType.class)
        String couponType,

        @NotNull(message = "할인가 지정은 필수")
        Long discountAmount,

        Long maxDiscountAmount,

        Long minPaymentAmount,

        @NotNull(message = "쿠폰 만료일자 필수")
        LocalDateTime expiredAt
) {
}
