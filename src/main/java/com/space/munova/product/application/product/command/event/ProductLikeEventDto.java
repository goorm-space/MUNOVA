package com.space.munova.product.application.product.command.event;

public record ProductLikeEventDto(Long productId, boolean isDeleted) {
}
