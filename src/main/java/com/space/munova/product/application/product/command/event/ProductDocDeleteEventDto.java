package com.space.munova.product.application.product.command.event;

import java.util.List;

public record ProductDocDeleteEventDto(List<Long> productIds, boolean isDeleted) {
}
