package com.space.munova.product.application.dto;

import com.space.munova.product.application.DeleteProductDetailDto;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateProductRequestDto (Long productId,
                                      boolean isDeletedMainImg,
                                      List<Long> deletedImgIds,
                                      @NotNull String ProductName,
                                      @NotNull Long price,
                                      @NotNull String info,
                                      AddShoeOptionDto addShoeOptionDto,
                                       UpdateQuantityDto updateQuantityDto,
                                       DeleteProductDetailDto deleteProductDetailDto
){

}
