package com.space.munova.product.application.dto;


public record ProductDetailAndSizeDto (Long productDetailId,
                                       Long sizeOptionId,
                                       String optionType,
                                       String size,
                                       int quantity){
}
