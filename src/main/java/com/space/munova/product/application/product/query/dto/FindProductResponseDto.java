package com.space.munova.product.application.product.query.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import lombok.Builder;

import java.time.LocalDateTime;


@Builder
public record FindProductResponseDto(Long productId,
                                     String mainImgSrc,
                                     String brandName,
                                     String productName,
                                     Long price,
                                     Integer likeCount,
                                     Integer salesCount,
                                     Integer viewCount,
                                     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                                     LocalDateTime createAt){


    /// 엘라스틱용 정적 팩토리 메소드
    public static FindProductResponseDto from(ProductEsDocument productEsDocument) {
        return FindProductResponseDto.builder()
                .productId(productEsDocument.getProductId())
                .mainImgSrc(productEsDocument.getMainImageUrl())
                .brandName(productEsDocument.getBrandName())
                .productName(productEsDocument.getName())
                .price(productEsDocument.getPrice())
                .likeCount(productEsDocument.getLikeCount())
                .salesCount(productEsDocument.getSalesCount())
                .viewCount(productEsDocument.getViewCount())
                .createAt(productEsDocument.getCreatedAt() != null ? productEsDocument.getCreatedAt().atStartOfDay() : null)
                .build();
    }

    /// 몽고용 정적 팩토리 메소드
    public static FindProductResponseDto from(ProductMongoDocument productMongoDocument) {

        return FindProductResponseDto.builder()
                .productId(productMongoDocument.getProductId())
                .mainImgSrc(productMongoDocument.getMainImage().getImageUrl())
                .brandName(productMongoDocument.getBrandName())
                .productName(productMongoDocument.getName())
                .price(productMongoDocument.getPrice())
                .likeCount(productMongoDocument.getLikeCount())
                .viewCount(productMongoDocument.getViewCount())
                .salesCount(productMongoDocument.getSalesCount())
                .createAt(productMongoDocument.getCreatedAt())
                .build();
    }
}
