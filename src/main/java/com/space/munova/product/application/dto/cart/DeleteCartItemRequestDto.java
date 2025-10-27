package com.space.munova.product.application.dto.cart;

import jakarta.validation.constraints.Size;

import java.util.List;

public record DeleteCartItemRequestDto(@Size(min = 1, message = "삭제할 카트 아이템을 최소 1개 이상 선택해주세요")
                                        List<Long> cartIds) {
}
