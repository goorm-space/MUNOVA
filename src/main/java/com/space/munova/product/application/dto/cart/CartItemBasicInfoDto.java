package com.space.munova.product.application.dto.cart;

public record CartItemBasicInfoDto (Long productId,
                                   Long cartId,
                                   Long detailId,
                                   String productName,
                                   String productPrice,
                                   String quantity,
                                   String mainImgSrc,
                                   String brandName){
}
