package com.space.munova.product.application.event;

import java.util.List;

public record ProductDeleteEvenForLikeDto(List<Long> productId, boolean isDeleted) {
}
