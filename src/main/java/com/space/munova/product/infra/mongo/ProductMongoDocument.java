package com.space.munova.product.infra.mongo;


import com.space.munova.product.application.product.command.dto.SavedDetailAndOptionInfoDto;
import com.space.munova.product.application.product.command.dto.UpdateQuantityDto;
import com.space.munova.product.application.product.command.event.ProductImageEventDto;
import com.space.munova.product.domain.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Document(collection = "product")
@CompoundIndex(name = "idx_complex_category_option_created_desc_id_desc"
        , def = "{'categoryId': 1, 'optionIds': 1, 'createdAt': -1, '_id': -1}")
@CompoundIndex(name = "idx_complex_category_option_likeCount_desc_id_desc"
        , def = "{'categoryId': 1, 'optionIds': 1, 'likeCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_complex_category_option_viewCount_desc_id_desc"
        , def = "{'categoryId': 1, 'optionIds': 1, 'viewCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_complex_category_option_salesCount_desc_id_desc"
        , def = "{'categoryId': 1, 'optionIds': 1, 'salesCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_option_created_desc_id_desc"
        , def = "{'optionIds': 1, 'createdAt': -1, '_id': -1}")
@CompoundIndex(name = "idx_option_likeCount_desc_id_desc"
        , def = "{'optionIds': 1, 'likeCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_option_viewCount_desc_id_desc"
        , def = "{'optionIds': 1, 'viewCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_option_salesCount_desc_id_desc"
        , def = "{'optionIds': 1, 'salesCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_category_created_desc_id_desc"
        , def = "{'categoryId': 1, 'createdAt': -1, '_id': -1}")
@CompoundIndex(name = "idx_category_likeCount_desc_id_desc"
        , def = "{'categoryId': 1, 'likeCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_category_viewCount_desc_id_desc"
        , def = "{'categoryId': 1, 'viewCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_category_salesCount_desc_id_desc"
        , def = "{'categoryId': 1, 'salesCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_created_desc_id_desc"
        , def = "{'createdAt': -1, '_id': -1}")
@CompoundIndex(name = "idx_likeCount_desc_id_desc"
        , def = "{'likeCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_viewCount_desc_id_desc"
        , def = "{'viewCount': -1, '_id': -1}")
@CompoundIndex(name = "idx_salesCount_desc_id_desc"
        , def = "{'salesCount': -1, '_id': -1}")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductMongoDocument {

    /// 몽고에서는 _id 로 매핑됨.
    ///  _id -> productId
    @Id
    private Long productId;

    @Field("name")
    private String name;

    @Field("info")
    private String info;

    @Field("price")
    private Long price;

    @Field("brandId")
    private Long brandId;

    @Field("brandName")
    private String brandName;

    @Field("categoryId")
    private Long categoryId;

    @Field("categoryName")
    private String categoryName;

    @Field("mainImage")
    private ProductImageMongoDocument mainImage;

    @Field("sideImages")
    private List<ProductImageMongoDocument> sideImages;

    @Field("likeCount")
    private Integer likeCount;

    @Field("salesCount")
    private Integer salesCount;

    @Field("viewCount")
    private Integer viewCount;

    @Field("isDeleted")
    private Boolean isDeleted;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;

    @Field("productDetails")
    private List<ProductDetailMongoDocument> productDetails;

    @Field("optionNames")
    private List<String> optionNames;

    @Field("optionIds")
    private List<Long> optionIds;

    // ProductDetail 내부 클래스
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetailMongoDocument {
        private Long productDetailId;
        private Integer quantity;
        private Boolean isDeleted;
        private List<OptionMongoDocument> options;
    }

    // Option 내부 클래스
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionMongoDocument {
        private Long optionId;
        private String optionName;
        private String optionType;
    }

    // ProductImage 내부 클래스 (이미지 ID와 URL 매핑)
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageMongoDocument {
        private Long productImageId;
        private String imageUrl;
    }

    public void updateStats(Integer likeCount
            , Integer viewCount
            , Integer salesCount) {

        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
    }

    /// 상품업데이트 정적 팩토리메서드
    public static ProductMongoDocument fromUpdate(
            ProductMongoDocument existingDoc,
            Long productId,
            String productName,
            String info,
            Long price,
            ProductImageEventDto updatedMainImg,
            List<ProductImageEventDto> addSideImages,
            List<ProductImageEventDto> removeSideImages,
            SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto,
            List<UpdateQuantityDto> updateQuantityDtos,
            List<Long> deleteDetailIds) {

        // 기존 문서를 기반으로 빌더 생성 (name, info, price, brand, category 등 유지)
        ProductMongoDocument.ProductMongoDocumentBuilder builder = existingDoc.toBuilder();

        // 이름, 설명, 가격 업데이트
        if (productName != null) {
            builder.name(productName);
        }
        if (info != null) {
            builder.info(info);
        }
        if (price != null) {
            builder.price(price);
        }

        // mainImage 있으면 업데이트
        if (updatedMainImg != null && !Boolean.TRUE.equals(updatedMainImg.isDeleted())) {
            ProductImageMongoDocument mainImageObj = ProductImageMongoDocument.builder()
                    .productImageId(updatedMainImg.id())
                    .imageUrl(updatedMainImg.imgUrl())
                    .build();
            builder.mainImage(mainImageObj);
        }

        // SideImages 업데이트 (기존 + 추가 - 삭제)
        List<ProductImageMongoDocument> sideImagesList = new ArrayList<>(
                existingDoc.getSideImages() != null ? existingDoc.getSideImages() : new ArrayList<>()
        );

        // 삭제할 이미지 제거
        if (removeSideImages != null && !removeSideImages.isEmpty()) {
            List<Long> removeImageIds = removeSideImages.stream()
                    .map(com.space.munova.product.application.product.command.event.ProductImageEventDto::id)
                    .toList();
            sideImagesList.removeIf(img -> removeImageIds.contains(img.getProductImageId()));
        }

        // 추가할 이미지 추가
        if (addSideImages != null && !addSideImages.isEmpty()) {
            for (com.space.munova.product.application.product.command.event.ProductImageEventDto img : addSideImages) {
                if (!Boolean.TRUE.equals(img.isDeleted())) {
                    ProductImageMongoDocument imgDoc = ProductImageMongoDocument.builder()
                            .productImageId(img.id())
                            .imageUrl(img.imgUrl())
                            .build();
                    // 중복 체크 (같은 ID가 이미 있으면 추가 안 함)
                    if (sideImagesList.stream().noneMatch(existing ->
                            existing.getProductImageId().equals(img.id()))) {
                        sideImagesList.add(imgDoc);
                    }
                }
            }
        }
        builder.sideImages(sideImagesList);

        // productDetails 업데이트
        List<ProductDetailMongoDocument> productDetailDocuments = new ArrayList<>(
                existingDoc.getProductDetails() != null ? existingDoc.getProductDetails() : new ArrayList<>()
        );

        // 삭제할 detail 제거
        if (deleteDetailIds != null && !deleteDetailIds.isEmpty()) {
            productDetailDocuments.removeIf(detail ->
                    deleteDetailIds.contains(detail.getProductDetailId()));
        }

        // 수량 업데이트
        if (updateQuantityDtos != null && !updateQuantityDtos.isEmpty()) {
            Map<Long, Integer> quantityMap = updateQuantityDtos.stream()
                    .collect(Collectors.toMap(UpdateQuantityDto::detailId, UpdateQuantityDto::quantity));

            // 기존 detail의 수량 업데이트
            for (int i = 0; i < productDetailDocuments.size(); i++) {
                ProductDetailMongoDocument detail = productDetailDocuments.get(i);
                Integer newQuantity = quantityMap.get(detail.getProductDetailId());
                if (newQuantity != null) {
                    // 수량만 업데이트
                    productDetailDocuments.set(i, ProductDetailMongoDocument.builder()
                            .productDetailId(detail.getProductDetailId())
                            .quantity(newQuantity)
                            .isDeleted(detail.getIsDeleted())
                            .options(detail.getOptions())
                            .build());
                }
            }
        }

        // 새로 추가된 detail 추가
        List<String> optionNamesList = new ArrayList<>(
                existingDoc.getOptionNames() != null ? existingDoc.getOptionNames() : new ArrayList<>()
        );
        List<Long> optionIdsList = new ArrayList<>(
                existingDoc.getOptionIds() != null ? existingDoc.getOptionIds() : new ArrayList<>()
        );

        if (savedDetailAndOptionInfoDto != null && savedDetailAndOptionInfoDto.savedProductDetails() != null) {
            // OptionMapping을 DetailId별로 그룹화 (productDetail null 안전 처리)
            Map<Long, List<ProductOptionMapping>> optionMappingsByDetailId = savedDetailAndOptionInfoDto
                    .savedProductOptionMappings()
                    .stream()
                    .filter(mapping -> mapping != null
                            && !mapping.isDeleted()
                            && mapping.getProductDetail() != null
                            && mapping.getProductDetail().getId() != null)
                    .collect(Collectors.groupingBy(
                            mapping -> mapping.getProductDetail().getId()
                    ));

            for (ProductDetail detail : savedDetailAndOptionInfoDto.savedProductDetails()) {
                if (detail == null || detail.getId() == null) {
                    continue; // detail이나 id가 없으면 스킵
                }
                List<OptionMongoDocument> optionDocuments = new ArrayList<>();

                // 해당 ProductDetail의 옵션 매핑 가져오기
                List<ProductOptionMapping> mappings = optionMappingsByDetailId.get(detail.getId());
                if (mappings != null) {
                    for (ProductOptionMapping mapping : mappings) {
                        if (mapping == null) continue;
                        Option option = mapping.getOption();
                        if (option != null) {
                            OptionMongoDocument optDoc = OptionMongoDocument.builder()
                                    .optionId(option.getId())
                                    .optionName(option.getOptionName())
                                    .optionType(option.getOptionType() != null
                                            ? option.getOptionType().name()
                                            : null)
                                    .build();
                            optionDocuments.add(optDoc);

                            // optionNames와 optionIds 수집 (중복 제거)
                            String optionName = option.getOptionName();
                            Long optionId = option.getId();
                            if (optionName != null && !optionNamesList.contains(optionName)) {
                                optionNamesList.add(optionName);
                            }
                            if (optionId != null && !optionIdsList.contains(optionId)) {
                                optionIdsList.add(optionId);
                            }
                        }
                    }
                }

                // 새 detail 추가
                productDetailDocuments.add(ProductDetailMongoDocument.builder()
                        .productDetailId(detail.getId())
                        .quantity(detail.getQuantity())
                        .isDeleted(detail.isDeleted())
                        .options(optionDocuments)
                        .build());
            }
        }

        builder.productDetails(productDetailDocuments);
        builder.optionNames(optionNamesList);
        builder.optionIds(optionIdsList);
        builder.updatedAt(LocalDateTime.now());  // 업데이트 시간 갱신

        return builder.build();
    }

    /// 상품저장 정적팩토리메서드
    public static ProductMongoDocument from(Product savedProduct,
                                            Brand brand,
                                            Category category,
                                            ProductImage mainImg,
                                            List<ProductImage> sideImgs,
                                            SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto) {

        // mainImage와 sideImages 변환
        ProductImageMongoDocument mainImageObj = null;
        List<ProductImageMongoDocument> sideImagesList = new ArrayList<>();

        if (mainImg != null && !mainImg.isDeleted()) {
            mainImageObj = ProductImageMongoDocument.builder()
                    .productImageId(mainImg.getId())
                    .imageUrl(mainImg.getImgUrl())
                    .build();
        }

        if (sideImgs != null) {
            for (ProductImage img : sideImgs) {
                if (!img.isDeleted()) {
                    ProductImageMongoDocument imgDoc = ProductImageMongoDocument.builder()
                            .productImageId(img.getId())
                            .imageUrl(img.getImgUrl())
                            .build();
                    sideImagesList.add(imgDoc);
                }
            }
        }

        // productDetails와 options 변환
        List<ProductDetailMongoDocument> productDetailDocuments = new ArrayList<>();
        List<String> optionNamesList = new ArrayList<>();
        List<Long> optionIdsList = new ArrayList<>();

        // ProductDetail별로 OptionMapping 그룹화
        Map<Long, List<ProductOptionMapping>> optionMappingsByDetailId = savedDetailAndOptionInfoDto
                .savedProductOptionMappings()
                .stream()
                .filter(mapping -> !mapping.isDeleted())
                .collect(Collectors.groupingBy(
                        mapping -> mapping.getProductDetail().getId()
                ));

        if (savedDetailAndOptionInfoDto.savedProductDetails() != null) {
            for (ProductDetail detail : savedDetailAndOptionInfoDto.savedProductDetails()) {
                List<OptionMongoDocument> optionDocuments = new ArrayList<>();

                // 해당 ProductDetail의 옵션 매핑 가져오기
                List<ProductOptionMapping> mappings = optionMappingsByDetailId.get(detail.getId());
                if (mappings != null) {
                    for (ProductOptionMapping mapping : mappings) {
                        Option option = mapping.getOption();
                        if (option != null) {
                            OptionMongoDocument optDoc = OptionMongoDocument.builder()
                                    .optionId(option.getId())
                                    .optionName(option.getOptionName())
                                    .optionType(option.getOptionType() != null
                                            ? option.getOptionType().name()
                                            : null)
                                    .build();
                            optionDocuments.add(optDoc);

                            // optionNames와 optionIds 수집 (중복 제거)
                            String optionName = option.getOptionName();
                            Long optionId = option.getId();
                            if (optionName != null && !optionNamesList.contains(optionName)) {
                                optionNamesList.add(optionName);
                            }
                            if (optionId != null && !optionIdsList.contains(optionId)) {
                                optionIdsList.add(optionId);
                            }
                        }
                    }
                }

                productDetailDocuments.add(ProductDetailMongoDocument.builder()
                        .productDetailId(detail.getId())
                        .quantity(detail.getQuantity())
                        .isDeleted(detail.isDeleted())
                        .options(optionDocuments)
                        .build());
            }
        }

        // createdAt과 updatedAt 변환
        LocalDateTime createdDate = savedProduct.getCreatedAt() != null
                ? savedProduct.getCreatedAt()
                : null;
        LocalDateTime updatedDate = savedProduct.getUpdatedAt() != null
                ? savedProduct.getUpdatedAt()
                : null;

        return ProductMongoDocument.builder()
                .productId(savedProduct.getId())
                .name(savedProduct.getName())
                .info(savedProduct.getInfo())
                .price(savedProduct.getPrice())
                .brandId(brand != null ? brand.getId() : null)
                .brandName(brand != null ? brand.getBrandName() : null)
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .mainImage(mainImageObj)
                .sideImages(sideImagesList)
                .likeCount(savedProduct.getLikeCount())
                .salesCount(savedProduct.getSalesCount())
                .viewCount(savedProduct.getViewCount())
                .isDeleted(savedProduct.isDeleted())
                .createdAt(createdDate)
                .updatedAt(updatedDate)
                .productDetails(productDetailDocuments)
                .optionNames(optionNamesList)
                .optionIds(optionIdsList)
                .build();
    }


}
