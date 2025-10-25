package com.space.munova.product.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record addCartItemRequestDto(Long productDetailId,
                                    @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
                                    int quantity) {
}
