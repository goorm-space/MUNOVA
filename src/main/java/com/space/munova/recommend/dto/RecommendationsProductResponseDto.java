package com.space.munova.recommend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationsProductResponseDto {
    private Long sourceProductId;
    private Long targetProductId;
    private LocalDateTime createdAt;
}
