package com.space.munova.product.application.product.command.event;

import com.space.munova.product.domain.enums.ProductImageType;

public record ProductImageEventDto(
        Long id,
        String imgUrl,
        ProductImageType imageType,
        Boolean isDeleted
) {}
