package com.space.munova.product.infra.mongo;


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
@Builder
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


    public static ProductMongoDocument from(Product product,
                                            String brandName,
                                            String categoryName,
                                            List<ProductImage> productImages,
                                            List<ProductDetail> productDetails,
                                            Map<Long, List<ProductOptionMapping>> optionMappingsByDetailId,
                                            Map<Long, Option> optionMap) {
        // mainImage와 sideImages 추출
        ProductImageMongoDocument mainImageObj = null;
        List<ProductImageMongoDocument> sideImagesList = new ArrayList<>();

        if (productImages != null) {
            for (ProductImage img : productImages) {
                if (!img.isDeleted()) {
                    ProductImageMongoDocument imgDoc = ProductImageMongoDocument.builder()
                            .productImageId(img.getId())
                            .imageUrl(img.getImgUrl())
                            .build();

                    if (img.getImageType() != null && img.getImageType().name().equals("MAIN")) {
                        mainImageObj = imgDoc;
                    } else {
                        sideImagesList.add(imgDoc);
                    }
                }
            }
        }

        // productDetails와 options 수집
        List<ProductDetailMongoDocument> productDetailDocuments = new ArrayList<>();
        List<String> optionNamesList = new ArrayList<>();
        List<Long> optionIdsList = new ArrayList<>();

        if (productDetails != null) {
            for (ProductDetail detail : productDetails) {
                List<OptionMongoDocument> optionDocuments = new ArrayList<>();

                // 해당 ProductDetail의 옵션 매핑 가져오기
                List<ProductOptionMapping> mappings = optionMappingsByDetailId.get(detail.getId());
                if (mappings != null) {
                    for (ProductOptionMapping mapping : mappings) {
                        if (!mapping.isDeleted()) {
                            Option option = optionMap.get(mapping.getOption().getId());
                            if (option != null) {
                                OptionMongoDocument optDoc = OptionMongoDocument.builder()
                                        .optionId(option.getId())
                                        .optionName(option.getOptionName())
                                        .optionType(option.getOptionType() != null
                                                ? option.getOptionType().name()
                                                : null)
                                        .build();
                                optionDocuments.add(optDoc);

                                // optionNames와 optionIds 수집
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
                }

                productDetailDocuments.add(ProductDetailMongoDocument.builder()
                        .productDetailId(detail.getId())
                        .quantity(detail.getQuantity())
                        .isDeleted(detail.isDeleted())
                        .options(optionDocuments)
                        .build());
            }
        }

        // createdAt과 updatedAt을 LocalDate로 변환
        LocalDateTime createdDate = product.getCreatedAt() != null
                ? product.getCreatedAt()
                : null;
        LocalDateTime updatedDate = product.getUpdatedAt() != null
                ? product.getUpdatedAt()
                : null;

        return ProductMongoDocument.builder()
                .productId(product.getId())
                .name(product.getName())
                .info(product.getInfo())
                .price(product.getPrice())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(brandName)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(categoryName)
                .mainImage(mainImageObj)
                .sideImages(sideImagesList)
                .likeCount(product.getLikeCount())
                .salesCount(product.getSalesCount())
                .viewCount(product.getViewCount())
                .isDeleted(product.isDeleted())
                .createdAt(createdDate)
                .updatedAt(updatedDate)
                .productDetails(productDetailDocuments)
                .optionNames(optionNamesList)
                .optionIds(optionIdsList)
                .build();
    }
}
