package com.space.munova.product.infra.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatsSyncDto {
    private Long productId;
    private Integer likeCount;
    private Integer viewCount;
    private Integer salesCount;
}
