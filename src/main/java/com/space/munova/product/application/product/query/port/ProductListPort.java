package com.space.munova.product.application.product.query.port;

import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductListPort {
    Page<FindProductResponseDto> findList(ProductSearchRequestDto request, Pageable pageable);

    boolean supports(ProductSearchRequestDto request);
}
