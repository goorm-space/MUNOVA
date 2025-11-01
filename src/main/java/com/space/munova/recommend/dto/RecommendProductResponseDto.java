package com.space.munova.recommend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;


public record RecommendProductResponseDto(
        Long productId,          // 상품 ID
        Long categoryId,         // 상품의 카테고리 ID
        String brandName,        // 브랜드명
        String productName,      // 상품명
        String mainImgUrl,       // 대표 이미지 URL
        Long price,              // 가격
        Integer likeCount,       // 좋아요 수
        Integer viewCount,       // 조회수
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDateTime createdAt  // 등록일
) {}