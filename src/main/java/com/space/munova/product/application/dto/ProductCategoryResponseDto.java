package com.space.munova.product.application.dto;

public record ProductCategoryResponseDto (Long id,
                                          String categoryName,
                                          Long parentId,
                                          int level){
}
