package com.space.munova.product.application.event;

import java.util.List;

public record ProductLikeEventDto(Long productId, boolean isDeleted) {
}
