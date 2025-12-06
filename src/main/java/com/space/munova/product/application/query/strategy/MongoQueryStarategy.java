package com.space.munova.product.application.query.strategy;

import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductCursorDto;
import com.space.munova.product.application.dto.ProductSearchRequestDto;
import com.space.munova.product.infra.mongo.query.ProductMongoQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoQueryStarategy implements SearchStrategy {

    private final ProductMongoQueryRepo productMongoQueryRepo;

    /// 전체 검색. 키워드 x
    @Override
    public Page<FindProductResponseDto> search(ProductSearchRequestDto req, Pageable pageable) {

        ProductCursorDto cursorDto = req.toCursorDto();

        Page<FindProductResponseDto> values = productMongoQueryRepo.findByConditions(req.getSortFlag(),
                cursorDto,
                req.getCategoryId(),
                req.getOptionIds(),
                pageable);



        return values;
    }

    @Override
    public boolean supports(ProductSearchRequestDto request) {
        return request.getKeyword() == null || request.getKeyword().isBlank();
    }
}
