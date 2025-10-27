package com.space.munova.product.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record FindProductResponseDto(Long productId,
                                     String mainImgSrc,
                                     String brandName,
                                     String productName,
                                     Long price,
                                     Integer likeCount,
                                     Integer salesCount,
                                     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                                     LocalDateTime createAt){
}
