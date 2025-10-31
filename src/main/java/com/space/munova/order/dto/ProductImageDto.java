package com.space.munova.order.dto;

import com.space.munova.product.domain.ProductImage;

public record ProductImageDto(
        Long imageId,
        String savedName,
        String imageType
) {
    public static ProductImageDto from(ProductImage productImage) {
        return new ProductImageDto(
                productImage.getId(),
                productImage.getImgUrl(),
                productImage.getImageType().name()
        );
    }
}
