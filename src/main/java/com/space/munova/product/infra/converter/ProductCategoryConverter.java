package com.space.munova.product.infra.converter;

import com.space.munova.product.domain.enums.ProductCategory;
import jakarta.persistence.AttributeConverter;

public class ProductCategoryConverter implements AttributeConverter<ProductCategory, String> {

    ///  DB에 저장할때 enum 타입에서 String 타입으로 형변환
    @Override
    public String convertToDatabaseColumn(ProductCategory productCategory) {
        return productCategory.name();
    }

    /// DB 조회해서 enum타입으로 형변환
    @Override
    public ProductCategory convertToEntityAttribute(String s) {
        return ProductCategory.valueOf(s);
    }
}
