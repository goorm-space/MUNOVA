package com.space.munova.product.application.cart.query.dto;

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
