package com.space.munova.product.application.dto.cart;

public record CartItemOptionInfoDto (Long detailId,
                                     Long optionId,
                                     String OptionType,
                                     String OptionName){
}
