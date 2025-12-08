package com.space.munova.product.application.product.command.event;

import java.util.List;

public record ProductDeleteEvenForLikeDto(List<Long> productId, boolean isDeleted) {
}