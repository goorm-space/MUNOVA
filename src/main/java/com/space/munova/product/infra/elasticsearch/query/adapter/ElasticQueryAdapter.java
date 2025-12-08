package com.space.munova.product.infra.elasticsearch.query.adapter;

import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductCursorDto;
import com.space.munova.product.application.product.query.dto.ProductSearchRequestDto;
import com.space.munova.product.application.product.query.port.ProductListPort;
import com.space.munova.product.infra.elasticsearch.query.ProductEsQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElasticQueryAdapter implements ProductListPort {

    private final ProductEsQueryRepo productEsQueryRepo;

    @Override
    public Page<FindProductResponseDto> findList(ProductSearchRequestDto req, Pageable pageable) {

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
