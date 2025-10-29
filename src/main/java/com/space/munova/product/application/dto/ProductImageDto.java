package com.space.munova.product.application.dto;

import com.space.munova.product.domain.enums.ProductImageType;

import java.util.List;

public record ProductImageDto (Long mainImgId,
                               String mainImgSrc,
                               List<ProductSideImgInfoDto> sideImgSrc) {
}
