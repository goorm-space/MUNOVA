package com.space.munova.product.application.dto;

import com.space.munova.product.domain.enums.OptionCategory;

public record ProductDetailAndSizeDto (Long productDetailId,
                                       Long sizeOptionId,
                                       String optionType,
                                       String size,
                                       int quantity){
}
