package com.space.munova.product.application.cart.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCartRequestDto(@NotNull Long cartId,
                                   @NotNull Long detailId,
                                   @NotNull Integer quantity) {
}
