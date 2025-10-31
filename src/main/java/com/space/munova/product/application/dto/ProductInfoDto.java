package com.space.munova.product.application.dto;

public record ProductInfoDto (Long productId,
                              Long categoryId,
                              String brandName,
                              String productName,
                              String productInfo,
                              Long productPrice,
                              int likeCount,
                              int viewCount) {
}
