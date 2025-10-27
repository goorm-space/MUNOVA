package com.space.munova.product.application.dto.cart;

public record CartItemBasicInfoDto (Long productId,
                                    Long cartId,
                                    Long detailId,
                                    String productName,
                                    Long productPrice,
                                    int productQuantity,
                                    int cartItemQuantity,
                                    String mainImgSrc,
                                    String brandName){
}
