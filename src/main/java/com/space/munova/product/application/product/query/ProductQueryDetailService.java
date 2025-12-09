package com.space.munova.product.application.product.query;

import com.space.munova.product.application.product.query.dto.ProductDetailInfoDto;
import com.space.munova.product.application.product.query.dto.ProductDetailOptions;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// mysql 읽기 전용 상품 디테일 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryDetailService {
    private final ProductDetailRepository productDetailRepository;

    //*
    // 상품아이디를 통해 상품디테일에 종속된 옵션 조회후 상품상세조회를 위한 DTO로 분류하여 반환 메서드
    // @parma - productId
    // */
    public List<ProductDetailInfoDto> findProductDetailInfoDtoByProductId(Long productId) {
        ///  1급 컬랙션으로 만들어버림.
        ProductDetailOptions productDetailOptions = new ProductDetailOptions(productDetailRepository.findProductDetailAndOptionsByProductId(productId));

        return  productDetailOptions.toProductDetailInfoList();
    }

}
