package com.space.munova.product.domain.product;

import com.space.munova.product.domain.product.enums.ProductImageType;
import jakarta.persistence.AttributeConverter;

public class ProductImageTypeConverter implements AttributeConverter<ProductImageType, String> {
    @Override
    public String convertToDatabaseColumn(ProductImageType productImageType) {
        return productImageType.name();
    }

    @Override
    public ProductImageType convertToEntityAttribute(String s) {
        return ProductImageType.valueOf(s);
    }
}

