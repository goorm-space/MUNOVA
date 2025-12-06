package com.space.munova.product.application.product.query;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductInfoDto;
import com.space.munova.product.application.product.query.exception.ProductException;
import com.space.munova.product.domain.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// mysql 읽기전요 상품 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;

    ///  판매다 등록상품 리스트 조회
    public PagingResponse<FindProductResponseDto> findProductBySeller(Pageable pageable, Long sellerId) {

        Page<FindProductResponseDto> value = productRepository.findProductBySeller(pageable, sellerId);
        return PagingResponse.from(value);
    }

    /// 판매자 등록상품상세 조회
    public ProductInfoDto findProductByIdAndSellerId(Long productId, Long sellerId) {
        return productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> ProductException.badRequestException("등록한 상품을 찾을 수 없습니다."));
    }


}
