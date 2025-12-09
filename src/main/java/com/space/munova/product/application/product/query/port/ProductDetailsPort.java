package com.space.munova.product.application.product.query.port;

import com.space.munova.product.application.product.query.dto.ProductDetailResponseDto;

public interface ProductDetailsPort {
    ProductDetailResponseDto findProductDetails(Long productId);
}
