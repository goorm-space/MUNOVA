package com.space.munova.product.application.command.dto;


import com.space.munova.product.application.dto.AddShoeOptionDto;
import com.space.munova.product.application.dto.ShoeOptionDto;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddProductRequestDto(@NotNull String ProductName,
                                   @NotNull Long price,
                                   @NotNull String info,
                                   @NotNull Long categoryId,
                                   @NotNull Long brandId,
                                   AddShoeOptionDto shoeOptionDto,
                                   List<ShoeOptionDto> shoeOptionDtos
) {

}
