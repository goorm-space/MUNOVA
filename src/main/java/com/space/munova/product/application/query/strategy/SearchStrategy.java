package com.space.munova.product.application.query.strategy;

import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchStrategy {
    Page<FindProductResponseDto> search(ProductSearchRequestDto request, Pageable pageable);

    boolean supports(ProductSearchRequestDto request);
}
