package com.space.munova.order.dto.redis;

import com.space.munova.order.entity.OrderItem;

public record TmpOrderItemDto(
        Long productDetailId,
        String productName,
        Long productPrice,
        Integer quantity
) {
    public static TmpOrderItemDto from(OrderItem orderItem) {
        return new TmpOrderItemDto(
                orderItem.getProductDetail().getId(),
                orderItem.getNameSnapshot(),
                orderItem.getPriceSnapshot(),
                orderItem.getQuantity()
        );
    }
}
