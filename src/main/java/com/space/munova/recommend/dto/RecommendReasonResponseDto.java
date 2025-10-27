package com.space.munova.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendReasonResponseDto {
    private String type;    // 예: "color", "category", "brand"
    private String reason; // 예: "비슷한 색상의 상품이에요."
}