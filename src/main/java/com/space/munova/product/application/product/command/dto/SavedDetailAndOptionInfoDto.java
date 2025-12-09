package com.space.munova.product.application.product.command.dto;

import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.ProductOptionMapping;

import java.util.List;

public record SavedDetailAndOptionInfoDto(List<ProductDetail> savedProductDetails,
                                          List<ProductOptionMapping> savedProductOptionMappings ) {
}
