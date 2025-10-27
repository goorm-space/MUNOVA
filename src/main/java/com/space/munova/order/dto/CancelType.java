package com.space.munova.order.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelType {
    ORDER_CANCEL("주문 취소"),
    RETURN_REFUND("환불");

    private final String description;
}
