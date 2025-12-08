package com.space.munova.product.application.product.command.event;

import com.space.munova.product.application.product.command.dto.SavedDetailAndOptionInfoDto;
import com.space.munova.product.application.product.command.dto.UpdateQuantityDto;

import java.util.List;

public record ProductUpdateEventDto(Long productId,
                                    String productName,
                                    Long price,
                                    String info,
                                    ProductImageEventDto updatedMainImg,
                                    List<ProductImageEventDto> addSideImages,
                                    List<ProductImageEventDto> removeSideImages,
                                    SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto,
                                    List<UpdateQuantityDto> updateQuantityDtos,
                                    List<Long> deleteDetailIds) {
}
