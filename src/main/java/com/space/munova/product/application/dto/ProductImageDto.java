package com.space.munova.product.application.dto;

import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.enums.ProductImageType;

import java.util.ArrayList;
import java.util.List;

public record ProductImageDto (Long mainImgId,
                               String mainImgSrc,
                               List<ProductSideImgInfoDto> sideImgSrc) {


    public static ProductImageDto fromProductImages(List<ProductImage> productImages) {
        String mainImgUrl = null;
        Long mainImgId = null;

        List<ProductSideImgInfoDto> sideImgInfoList = new ArrayList<>();

        if (productImages == null) {
            return new ProductImageDto(null, null, sideImgInfoList);
        }

        for(ProductImage img : productImages) {
            if(img.getImageType().equals(ProductImageType.MAIN)) {
                mainImgId = img.getId();
                mainImgUrl = img.getImgUrl();
            } else if(img.getImageType().equals(ProductImageType.SIDE)) {
                String sideImgUrl = img.getImgUrl();
                Long sideImgId = img.getId();
                ProductSideImgInfoDto sideImgInfo = new ProductSideImgInfoDto(sideImgId, sideImgUrl);
                sideImgInfoList.add(sideImgInfo);
            }
        }

        return new ProductImageDto(mainImgId, mainImgUrl, sideImgInfoList);
    }
}
