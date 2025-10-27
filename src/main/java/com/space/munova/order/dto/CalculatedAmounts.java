package com.space.munova.order.dto;

public record CalculatedAmounts(
        Long productAmount,
        int discountAmount,
        Long finalAmount
) {
}
