package com.space.munova.product.infra.elasticsearch;

import com.space.munova.product.application.product.command.dto.SavedDetailAndOptionInfoDto;
import com.space.munova.product.application.product.command.dto.UpdateQuantityDto;
import com.space.munova.product.application.product.command.event.ProductImageEventDto;
import com.space.munova.product.domain.*;
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
import java.util.ArrayList;
import java.util.List;

@Document(indexName = "product")
@Getter
@Builder(toBuilder = true)
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

    public void updateStats(Integer likeCount, Integer viewCount, Integer salesCount) {
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
    }

    /// 상품 업데이트 정적 팩토리 메서드
    public static ProductEsDocument fromUpdate(
            ProductEsDocument existingDoc,
            ProductImageEventDto updatedMainImg,
            SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto,
            String productName,
            Long price) {

        ProductEsDocument.ProductEsDocumentBuilder builder = existingDoc.toBuilder();

        // 이름/가격 업데이트
        if (productName != null) {
            builder.name(productName);
        }
        if (price != null) {
            builder.price(price);
        }

        // mainImageUrl 업데이트
        if (updatedMainImg != null && !Boolean.TRUE.equals(updatedMainImg.isDeleted())) {
            builder.mainImageUrl(updatedMainImg.imgUrl());
        }

        // ptionNames, optionIds 업데이트 (새로 추가된 옵션만 추가)
        List<String> optionNamesList = new ArrayList<>();
        List<Long> optionIdsList = new ArrayList<>();

        // 기존 optionNames 파싱
        if (existingDoc.getOptionNames() != null && !existingDoc.getOptionNames().isEmpty()) {
            optionNamesList.addAll(List.of(existingDoc.getOptionNames().split(" ")));
        }

        // 기존 optionIds 추가
        if (existingDoc.getOptionIds() != null) {
            optionIdsList.addAll(existingDoc.getOptionIds());
        }

        // 새로 추가된 옵션 수집
        if (savedDetailAndOptionInfoDto != null &&
                savedDetailAndOptionInfoDto.savedProductOptionMappings() != null) {

            for (ProductOptionMapping mapping : savedDetailAndOptionInfoDto.savedProductOptionMappings()) {
                if (!mapping.isDeleted()) {
                    Option option = mapping.getOption();
                    if (option != null) {
                        String optionName = option.getOptionName();
                        Long optionId = option.getId();

                        // optionNames 중복 제거하며 추가
                        if (optionName != null && !optionNamesList.contains(optionName)) {
                            optionNamesList.add(optionName);
                        }

                        // optionIds 중복 제거하며 추가
                        if (optionId != null && !optionIdsList.contains(optionId)) {
                            optionIdsList.add(optionId);
                        }
                    }
                }
            }
        }

        // optionNames를 공백으로 구분된 문자열로 변환
        String updatedOptionNames = optionNamesList.isEmpty()
                ? null
                : String.join(" ", optionNamesList);

        builder.optionNames(updatedOptionNames);
        builder.optionIds(optionIdsList.isEmpty() ? null : optionIdsList);

        return builder.build();
    }

    /// 상품 저장 정적 팩토리 메서드
    public static ProductEsDocument from(Product savedProduct,
                                         Brand brand,
                                         Category category,
                                         ProductImage mainImg,
                                         SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto) {

        // mainImageUrl 추출
        String mainImageUrl = null;
        if (mainImg != null && !mainImg.isDeleted()) {
            mainImageUrl = mainImg.getImgUrl();
        }

        // optionNames와 optionIds 수집
        List<String> optionNamesList = new ArrayList<>();
        List<Long> optionIdsList = new ArrayList<>();

        // ProductOptionMapping에서 Option 추출
        if (savedDetailAndOptionInfoDto.savedProductOptionMappings() != null) {
            for (ProductOptionMapping mapping : savedDetailAndOptionInfoDto.savedProductOptionMappings()) {
                if (!mapping.isDeleted()) {
                    Option option = mapping.getOption();
                    if (option != null) {
                        String optionName = option.getOptionName();
                        Long optionId = option.getId();

                        // optionNames 중복 제거하며 수집
                        if (optionName != null && !optionNamesList.contains(optionName)) {
                            optionNamesList.add(optionName);
                        }

                        // optionIds 중복 제거하며 수집
                        if (optionId != null && !optionIdsList.contains(optionId)) {
                            optionIdsList.add(optionId);
                        }
                    }
                }
            }
        }

        // optionNames를 공백으로 구분된 문자열로 변환 (Elasticsearch 검색용)
        String optionNames = optionNamesList.isEmpty()
                ? null
                : String.join(" ", optionNamesList);

        // createdAt을 LocalDate로 변환
        LocalDate createdDate = savedProduct.getCreatedAt() != null
                ? savedProduct.getCreatedAt().toLocalDate()
                : null;

        return ProductEsDocument.builder()
                .productId(savedProduct.getId())
                .name(savedProduct.getName())
                .price(savedProduct.getPrice())
                .brandId(brand != null ? brand.getId() : null)
                .brandName(brand != null ? brand.getBrandName() : null)
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .mainImageUrl(mainImageUrl)
                .optionNames(optionNames)
                .optionIds(optionIdsList.isEmpty() ? null : optionIdsList)
                .likeCount(savedProduct.getLikeCount())
                .salesCount(savedProduct.getSalesCount())
                .viewCount(savedProduct.getViewCount())
                .createdAt(createdDate)
                .build();
    }


//    public static ProductEsDocument from(
//            Product product,
//            String brandName,
//            String categoryName,
//            String optionNames,
//            String mainImageUrl,
//            List<Long> optionIds
//    ) {
//        return ProductEsDocument.builder()
//                .productId(product.getId())
//                .name(product.getName())
//                .price(product.getPrice())
//                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
//                .brandName(brandName)
//                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
//                .categoryName(categoryName)
//                .mainImageUrl(mainImageUrl)
//                .optionNames(optionNames)
//                .optionIds(optionIds)
//                .likeCount(product.getLikeCount())
//                .salesCount(product.getSalesCount())
//                .viewCount(product.getViewCount())
//                .createdAt(product.getCreatedAt().toLocalDate())
//                .build();
//    }


}