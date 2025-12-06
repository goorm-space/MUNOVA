package com.space.munova.product.application.product.query.dto;

import com.space.munova.product.domain.enums.OptionCategory;
import com.space.munova.product.infra.mongo.ProductMongoDocument;


import java.util.ArrayList;
import java.util.List;


public record ProductDetailResponseDto(ProductInfoDto productInfoDto,
                                       ProductImageDto productImageDto,
                                       List<ProductDetailInfoDto> detailInfos
                                       ) {


    public static ProductDetailResponseDto from(ProductMongoDocument doc) {

        ProductInfoDto productInfoDto = ProductInfoDto.from(doc);
        ProductImageDto productImageDto = ProductImageDto.from(doc);
        List<ProductDetailInfoDto> detailInfoDtos = new ArrayList<>();

        seperateOptions(doc, detailInfoDtos);


        return new ProductDetailResponseDto(productInfoDto, productImageDto, detailInfoDtos);
    }


    private static void seperateOptions(ProductMongoDocument doc, List<ProductDetailInfoDto> detailInfoDtos) {
        List<ProductMongoDocument.ProductDetailMongoDocument> productDetails = doc.getProductDetails();

        for (ProductMongoDocument.ProductDetailMongoDocument pd : productDetails) {

            ColorOptionDto colorOptionDto = null;
            List<ProductDetailAndSizeDto>  productDetailAndSizeDtos = new ArrayList<>();

            for(ProductMongoDocument.OptionMongoDocument opt : pd.getOptions()) {

                if(opt.getOptionType().equals(OptionCategory.COLOR.toString())) {

                    colorOptionDto = new ColorOptionDto(opt.getOptionId(), opt.getOptionType(), opt.getOptionName());
                } else {
                    ProductDetailAndSizeDto productDetailAndSizeDto = new ProductDetailAndSizeDto(pd.getProductDetailId(),
                            opt.getOptionId(),
                            opt.getOptionType(),
                            opt.getOptionName(),
                            pd.getQuantity());

                    productDetailAndSizeDtos.add(productDetailAndSizeDto);
                }

                ProductDetailInfoDto productDetailInfoDto = new ProductDetailInfoDto(colorOptionDto, productDetailAndSizeDtos);
                detailInfoDtos.add(productDetailInfoDto);
            }

        }
    }


}
