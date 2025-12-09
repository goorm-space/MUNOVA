package com.space.munova.product.application.product.query.dto;

import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.enums.ProductImageType;
import com.space.munova.product.infra.mongo.ProductMongoDocument;

import java.util.ArrayList;
import java.util.List;

public record ProductImageDto (ProductImgInfoDto mainImgInfo, List<ProductImgInfoDto> sideImgInfos) {


    public static ProductImageDto from(ProductMongoDocument doc) {

        ProductImgInfoDto mainImgInfo = new ProductImgInfoDto(doc.getMainImage().getProductImageId(),
                doc.getMainImage().getImageUrl());

        List<ProductImgInfoDto> sideImgInfos = doc.getSideImages()
                .stream()
                .map(s -> {
                    return new ProductImgInfoDto(s.getProductImageId(), s.getImageUrl());
                })
                .toList();

        return new ProductImageDto(mainImgInfo, sideImgInfos);
    }

    public static ProductImageDto fromProductImages(List<ProductImage> productImages) {
        ProductImgInfoDto mainImginfo = null;
        List<ProductImgInfoDto> sideImgInfoList = new ArrayList<>();

        if (productImages == null) {
            return new ProductImageDto(null, sideImgInfoList);
        }

        for(ProductImage img : productImages) {
            if(img.getImageType().equals(ProductImageType.MAIN)) {
                mainImginfo = new ProductImgInfoDto(img.getId(), img.getImgUrl());
            } else if(img.getImageType().equals(ProductImageType.SIDE)) {
                String sideImgUrl = img.getImgUrl();
                Long sideImgId = img.getId();
                ProductImgInfoDto sideImgInfo = new ProductImgInfoDto(sideImgId, sideImgUrl);
                sideImgInfoList.add(sideImgInfo);
            }
        }

        return new ProductImageDto(mainImginfo, sideImgInfoList);
    }
}