package com.space.munova.product.application.dto.cart;

import com.space.munova.product.domain.enums.OptionCategory;

public record ProductInfoForCartDto(Long productId,
                                    Long cartId,
                                    Long detailId,
                                    String productName,
                                    Long productPrice,
                                    int productQuantity,
                                    int cartItemQuantity,
                                    String mainImgSrc,
                                    String brandName,
                                    Long optionId,
                                    OptionCategory optionType,
                                    String optionName) {
}
