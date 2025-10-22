package com.space.munova.product.application.dto;

import com.space.munova.product.domain.enums.ProductCategory;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddProductRequestDto (@NotNull String ProductName,
                                    @NotNull Long price,
                                    @NotNull String info,
                                    @NotNull Long categoryId,
                                    @NotNull List<ShoeOptionDto> shoeOptionDtos
                                    ){

}
