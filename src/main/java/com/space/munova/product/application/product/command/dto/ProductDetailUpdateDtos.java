package com.space.munova.product.application.product.command.dto;

import java.util.ArrayList;
import java.util.List;

public record ProductDetailUpdateDtos(List<ShoeOptionDto> addShoeOptionDtos
        , List<UpdateQuantityDto> updateQuantityDtos
        , List<Long> deleteDetailIds) {

    public static ProductDetailUpdateDtos from(UpdateProductRequestDto reqDto) {
        List<ShoeOptionDto> addShoeOptionDtos = reqDto.addShoeOptionDto() == null
                ? new ArrayList<>()
                : reqDto.addShoeOptionDto().shoeOptionDtos();
        List<UpdateQuantityDto> updateQuantityDtos = reqDto.updateQuantityDto() == null
                ? new ArrayList<>()
                : reqDto.updateQuantityDto();
        List<Long> deleteDetailIds = reqDto.deleteProductDetailDto() == null
                ? new ArrayList<>()
                : reqDto.deleteProductDetailDto().detailId();

        return new ProductDetailUpdateDtos(addShoeOptionDtos, updateQuantityDtos, deleteDetailIds);
    }

    public void removeDeletedItemsFromUpdateList() {
        if(this.updateQuantityDtos.isEmpty() || this.deleteDetailIds.isEmpty()) {
            return;
        }

        updateQuantityDtos.removeIf(dto -> deleteDetailIds.contains(dto.detailId()));
    }

}