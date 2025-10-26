package com.space.munova.product.application.dto.cart;

import jakarta.validation.constraints.Min;

public record AddCartItemRequestDto(Long productDetailId,
                                    @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
                                    int quantity) {
}
