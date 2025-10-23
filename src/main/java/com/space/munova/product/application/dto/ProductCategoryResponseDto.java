package com.space.munova.product.application.dto;

import com.space.munova.product.infra.ProductCategoryConverter;
import jakarta.persistence.Converter;

public record ProductCategoryResponseDto (Long id,
                                          String categoryName,
                                          int level){
}
