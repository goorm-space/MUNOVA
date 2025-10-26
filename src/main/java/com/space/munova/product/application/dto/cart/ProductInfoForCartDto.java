package com.space.munova.product.application.dto.cart;

public record ProductInfoForCartDto(Long detailId,
                                    Long productId,
                                    String productName,
                                    Long productPrice,
                                    String mainImgSrc,
                                    String brandName) {
}
