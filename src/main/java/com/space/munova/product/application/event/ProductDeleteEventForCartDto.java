package com.space.munova.product.application.event;

import java.util.List;

public record ProductDeleteEventForCartDto(List<Long> productDetailIds, boolean isDeleted) {
}
