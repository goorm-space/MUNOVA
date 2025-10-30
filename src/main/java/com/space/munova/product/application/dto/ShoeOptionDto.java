package com.space.munova.product.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ShoeOptionDto(Long colorId,
                            @NotNull(message = "컬러는 필수입니다.")
                            String color,
                            Long sizeId,
                            @NotNull(message = "사이즈는 필수입니다.")
                            String size,
                            @NotNull(message = "수량은 필수입니다.")
                            @PositiveOrZero(message = "수량은 0 이상이어야 합니다.")
                            int quantity) {

}
