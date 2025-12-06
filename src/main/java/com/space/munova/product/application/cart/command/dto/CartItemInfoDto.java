package com.space.munova.product.application.cart.command.dto;

public record CartItemInfoDto (Long cartId,
                               Long productDetailId,
                               int quantity){
}
