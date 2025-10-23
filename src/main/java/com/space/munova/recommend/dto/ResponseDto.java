package com.space.munova.recommend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDto {
    private Long productId;
    private String productName;
    private int score;
}