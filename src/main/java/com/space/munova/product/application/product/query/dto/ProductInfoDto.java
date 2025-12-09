package com.space.munova.product.application.product.query.dto;

import com.space.munova.product.infra.mongo.ProductMongoDocument;

public record ProductInfoDto (Long productId,
                              Long categoryId,
                              String brandName,
                              String productName,
                              String productInfo,
                              Long productPrice,
                              int likeCount,
                              int viewCount) {

    public static ProductInfoDto from(ProductMongoDocument doc) {
        return new ProductInfoDto(doc.getProductId(),
                doc.getCategoryId(),
                doc.getBrandName(),
                doc.getName(),
                doc.getInfo(),
                doc.getPrice(),
                doc.getLikeCount(),
                doc.getViewCount());
    }
}
