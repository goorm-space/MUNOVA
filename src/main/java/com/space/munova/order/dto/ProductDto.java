package com.space.munova.order.dto;

import com.space.munova.product.domain.Product;

import java.util.List;
import java.util.stream.Collectors;

public record ProductDto(
        Long productId,
        String name,
        Long price,
        BrandDto brand,
        List<ProductImageDto> images,
        List<OptionDto> options
) {
    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getBrand() != null ? BrandDto.from(product.getBrand()) : null,
                product.getProductImages() != null
                        ? product.getProductImages().stream().map(ProductImageDto::from).collect(Collectors.toList())
                        : List.of(),
                product.getProductDetails() != null
                        ? product.getProductDetails().stream()
                        .flatMap(detail -> detail.getOptionMappings().stream())
                        .map(mapping -> OptionDto.from(mapping.getOption()))
                        .collect(Collectors.toList())
                        : List.of()
        );
    }
}
