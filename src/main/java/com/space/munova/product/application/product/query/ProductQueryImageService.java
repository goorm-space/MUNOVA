package com.space.munova.product.application.product.query;

import com.space.munova.product.application.product.query.dto.ProductImageDto;
import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.Repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// 읽기전용 mysql 이미지 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryImageService {

    private final ProductImageRepository productImageRepository;

    public ProductImageDto findProductImageDtoByProductId(Long productId) {

        List<ProductImage> productImages = productImageRepository.findByProductId(productId);

        return ProductImageDto.fromProductImages(productImages);
    }
}
