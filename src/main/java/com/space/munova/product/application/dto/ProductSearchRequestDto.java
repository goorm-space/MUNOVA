package com.space.munova.product.application.dto;

import com.space.munova.product.application.command.exception.ProductException;
import com.space.munova.product.domain.enums.SortFlag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductSearchRequestDto {

    private SortFlag sortFlag = SortFlag.CREATED_AT;
    private Long cursorProductId;
    @DateTimeFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss]")
    private LocalDate cursorCreatedAt;
    private Integer cursorLikeCount;
    private Integer cursorViewCount;
    private Integer cursorSalesCount;
    private Long categoryId;
    private String keyword;
    private List<Long> optionIds;

    public ProductCursorDto toCursorDto() {
        if (cursorProductId == null) {
            return null;
        }

        if (!isValidCursor()) {
            throw ProductException.badRequestException("잘못된 요청입니다.");
        }

        ProductCursorDto productCursorDto = new ProductCursorDto();
        productCursorDto.setProductId(cursorProductId);

        switch (sortFlag) {
            case CREATED_AT -> productCursorDto.setCreatedAt(cursorCreatedAt);
            case SALES_COUNT -> productCursorDto.setSalesCount(cursorSalesCount);
            case LIKE_COUNT -> productCursorDto.setLikeCount(cursorLikeCount);
            case VIEW_COUNT -> productCursorDto.setViewCount(cursorViewCount);
        }

        return productCursorDto;
    }

    public boolean isValidCursor() {
        if (cursorProductId == null) {
            return true;
        }

        return switch (sortFlag) {
            case LIKE_COUNT -> cursorLikeCount != null;
            case VIEW_COUNT -> cursorViewCount != null;
            case SALES_COUNT -> cursorSalesCount != null;
            case CREATED_AT -> cursorCreatedAt != null;
        };
    }
}
