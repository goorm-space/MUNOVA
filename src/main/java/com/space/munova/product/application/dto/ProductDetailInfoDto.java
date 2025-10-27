package com.space.munova.product.application.dto;

import java.util.List;

public record ProductDetailInfoDto (ColorOptionDto colorOptionDto,
                                    List<ProductDetailAndSizeDto> productDetailAndSizeDtoList){
}
