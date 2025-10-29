package com.space.munova.product.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateProductRequestDto (Long productId,
                                      List<Long> deletedImgIds,
                                      @NotNull String ProductName,
                                      @NotNull Long price,
                                      @NotNull String info,
                                      @NotNull Long categoryId,
                                      @NotNull Long brandId,
                                      @NotNull List<ShoeOptionDto> shoeOptionDtos
){

}
