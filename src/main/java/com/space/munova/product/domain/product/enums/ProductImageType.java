package com.space.munova.product.domain.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductImageType {
    SIDE ("사이드_이미지"),
    MAIN ("메인_이미지");

    private String description;
}

