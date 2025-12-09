package com.space.munova.product.application.product.query.dto;

public record ProductCategoryResponseDto (Long id,
                                          String categoryName,
                                          Long parentId,
                                          int level){
}
