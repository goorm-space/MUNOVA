package com.space.munova.order.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
    CART("장바구니"),
    DIRECT("바로 구매");

    private final String description;
}
