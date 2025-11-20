package com.space.munova.order.service.processor;

import com.space.munova.common.validation.AmountVerifier;
import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponAppliedProcessor implements OrderAmountProcessor {

    private final CouponService couponService;

    @Override
    public void process(Order order, CreateOrderRequest request, long totalAmount) {
        UseCouponRequest couponRequest = UseCouponRequest.of(totalAmount);
        UseCouponResponse couponResponse = couponService.calculateAmountWithCoupon(request.orderCouponId(), couponRequest);

        try {
            AmountVerifier.verify(request.clientCalculatedAmount(), couponResponse.finalPrice());
        } catch (IllegalArgumentException e) {
            throw OrderException.amountMismatchException(e.getMessage());
        }

        order.updateOrder(
                couponResponse.originalPrice(),
                couponResponse.discountPrice(),
                couponResponse.finalPrice(),
                request.orderCouponId()
        );
    }
}
