package com.space.munova.product.application.product.command.event;

public record ProductDeleteEventDto (Long productId, boolean isDeleted) {
}
