package com.space.munova.product.application.product.command.event;

import com.space.munova.product.application.product.command.dto.SavedDetailAndOptionInfoDto;
import com.space.munova.product.application.product.command.dto.UpdateQuantityDto;
import com.space.munova.product.domain.ProductImage;

import java.util.List;

public record ProductUpdateEventDto (Long productId,
                                     ProductImage updatedMainImg,
                                     List<ProductImage> addSideImages,
                                     List<ProductImage> removeSideImages,
                                     SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto,
                                     List<UpdateQuantityDto> updateQuantityDtos,
                                     List<Long> deleteDetailIds){
}
