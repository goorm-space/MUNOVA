package com.space.munova.product.infra.converter;


import com.space.munova.product.domain.enums.OptionCategory;
import jakarta.persistence.AttributeConverter;

public class OptionConverter implements AttributeConverter<OptionCategory, String> {
    @Override
    public String convertToDatabaseColumn(OptionCategory optionCategory) {
        return optionCategory.name();
    }

    @Override
    public OptionCategory convertToEntityAttribute(String s) {
        return OptionCategory.valueOf(s);
    }
}
