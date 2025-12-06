package com.space.munova.product.application.product.query.dto;


public record ProductDetailAndSizeDto (Long productDetailId,
                                       Long sizeOptionId,
                                       String optionType,
                                       String size,
                                       int quantity){
}
