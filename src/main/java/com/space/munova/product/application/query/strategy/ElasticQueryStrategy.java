package com.space.munova.product.application.query.strategy;

import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductCursorDto;
import com.space.munova.product.application.dto.ProductSearchRequestDto;
import com.space.munova.product.infra.elasticsearch.query.ProductEsQueryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticQueryStrategy implements SearchStrategy {

    private final ProductEsQueryRepo productEsQueryRepo;

    @Override
    public Page<FindProductResponseDto> search(ProductSearchRequestDto req, Pageable pageable) {

        ProductCursorDto cursorDto = req.toCursorDto();

        Page<FindProductResponseDto> values = productEsQueryRepo.search(req.getSortFlag(),
                cursorDto,
                req.getCategoryId(),
                req.getOptionIds(),
                req.getKeyword(),
                pageable);


        return values;
    }

    @Override
    public boolean supports(ProductSearchRequestDto request) {
        return request.getKeyword() != null && !request.getKeyword().isBlank();
    }
}
