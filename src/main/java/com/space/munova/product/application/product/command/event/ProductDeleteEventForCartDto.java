package com.space.munova.product.application.product.command.event;

import java.util.List;

public record ProductDeleteEventForCartDto(List<Long> productDetailIds, boolean isDeleted) {
}
