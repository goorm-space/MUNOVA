package com.space.munova.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum SortFlag {

    SALES_COUNT("salesCount"),
    LIKE_COUNT("likeCount"),
    VIEW_COUNT("viewCount"),
    CREATED_AT("createdAt");

    private final String name;
}
