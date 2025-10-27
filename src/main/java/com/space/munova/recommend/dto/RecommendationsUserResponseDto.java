package com.space.munova.recommend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationsUserResponseDto {
    private Long memberId;
    private Long productId;
    private Double score;
    private LocalDateTime createdAt;
}
