package com.space.munova.order.dto;

import com.space.munova.product.domain.Brand;

public record BrandDto(
        Long brandId,
        String brandName
) {
    public static BrandDto from(Brand brand) {
        return new BrandDto(
                brand.getId(),
                brand.getBrandName()
        );
    }
}
