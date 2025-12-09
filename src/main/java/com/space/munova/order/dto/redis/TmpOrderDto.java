package com.space.munova.order.dto.redis;

import com.space.munova.order.entity.Order;

import java.util.ArrayList;
import java.util.List;

public record TmpOrderDto(
        Long memberId,
        String orderNum,
        String userRequest,
        Long couponId,
        Long originalPrice,
        Long discountPrice,
        Long finalPrice,
        List<TmpOrderItemDto> items
) {
    public static TmpOrderDto from(Long memberId, Order order) {
        List<TmpOrderItemDto> items = new ArrayList<>();
        order.getOrderItems().forEach(item -> {
            TmpOrderItemDto itemDto = TmpOrderItemDto.from(item);
            items.add(itemDto);
        });

        return new TmpOrderDto(
                memberId,
                order.getOrderNum(),
                order.getUserRequest(),
                order.getCouponId(),
                order.getOriginPrice(),
                order.getDiscountPrice(),
                order.getTotalPrice(),
                items
        );
    }
}
