package com.space.munova.product.application.product.query.dto;

import com.space.munova.product.domain.enums.SortFlag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCursorDto {

    private Long productId;
    private LocalDate createdAt;
    private Integer likeCount;
    private Integer viewCount;
    private Integer salesCount;

    ///  몽고, 엘라에서 소팅, 범위설정에 필요한 비교가능한 타입을 반환.
    public Comparable<?> getSortValue(SortFlag sortFlag) {
        return switch (sortFlag) {
            case LIKE_COUNT -> likeCount;
            case VIEW_COUNT -> viewCount;
            case SALES_COUNT -> salesCount;
            case CREATED_AT -> createdAt;
        };
    }
}
