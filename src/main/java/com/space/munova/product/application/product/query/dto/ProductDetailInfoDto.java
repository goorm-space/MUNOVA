package com.space.munova.product.application.product.query.dto;

import java.util.List;

public record ProductDetailInfoDto (ColorOptionDto colorOptionDto,
                                    List<ProductDetailAndSizeDto> productDetailAndSizeDtoList){


}
