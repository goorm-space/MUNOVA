package com.space.munova.product.application.cart.dto;

public record CartItemInfoDto (Long cartId,
                               Long productDetailId,
                               int quantity){
}
