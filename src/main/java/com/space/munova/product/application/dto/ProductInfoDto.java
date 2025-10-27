package com.space.munova.product.application.dto;

public record ProductInfoDto (Long productId,
                                String brandName,
                                String productName,
                                String productInfo,
                                Long productPrice) {
}
