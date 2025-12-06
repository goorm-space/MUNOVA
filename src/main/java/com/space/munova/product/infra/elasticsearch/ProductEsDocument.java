package com.space.munova.product.infra.elasticsearch;

import com.space.munova.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@Document(indexName = "product")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEsDocument {

    @Id
    @Field("productId")
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")  // 한국어 형태소 분석기 사용
    private String name;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")  // 브랜드명 검색용
    private String brandName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")  // 카테고리명 검색용
    private String categoryName;

    @Field(type = FieldType.Text)
    private String mainImageUrl;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    // 옵션명들 검색용 스탠다드로 설정 (영어 숫자 가주로 있기때문에)
    private String optionNames;

    @Field(type = FieldType.Long)
    private List<Long> optionIds;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    private Integer salesCount;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate createdAt;


    public static ProductEsDocument from(
            Product product,
            String brandName,
            String categoryName,
            String optionNames,
            String mainImageUrl,
            List<Long> optionIds
    ) {
        return ProductEsDocument.builder()
                .productId(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(brandName)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(categoryName)
                .mainImageUrl(mainImageUrl)
                .optionNames(optionNames)
                .optionIds(optionIds)
                .likeCount(product.getLikeCount())
                .salesCount(product.getSalesCount())
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt().toLocalDate())
                .build();
    }
}