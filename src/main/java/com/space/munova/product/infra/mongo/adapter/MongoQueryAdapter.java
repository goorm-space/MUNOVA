package com.space.munova.product.infra.mongo.adapter;

import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductCursorDto;
import com.space.munova.product.application.product.query.dto.ProductDetailResponseDto;
import com.space.munova.product.application.product.query.dto.ProductSearchRequestDto;
import com.space.munova.product.application.product.query.exception.ProductException;
import com.space.munova.product.application.product.query.port.ProductDetailsPort;
import com.space.munova.product.application.product.query.port.ProductListPort;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.product.infra.mongo.query.ProductMongoQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoQueryAdapter implements ProductListPort, ProductDetailsPort {

    private final ProductMongoQueryRepo productMongoQueryRepo;


    ///  상품 상세 조회.
    @Override
    public ProductDetailResponseDto findProductDetails(Long productId) {
        ProductMongoDocument doc = productMongoQueryRepo
                .findById(productId)
                .orElseThrow(() -> ProductException.badRequestException("상품 정보를 확인할 수 없습니다."));

        return ProductDetailResponseDto.from(doc);
    }


    ///  상품 목록 조회.
    @Override
    public Page<FindProductResponseDto> findList(ProductSearchRequestDto request, Pageable pageable) {
        ProductCursorDto cursorDto = request.toCursorDto();

        Page<FindProductResponseDto> values = productMongoQueryRepo.findByConditions(request.getSortFlag(),
                cursorDto,
                request.getCategoryId(),
                request.getOptionIds(),
                pageable);

        return values;
    }

    @Override
    public boolean supports(ProductSearchRequestDto request) {
        return request.getKeyword() == null || request.getKeyword().isBlank();
    }
}
