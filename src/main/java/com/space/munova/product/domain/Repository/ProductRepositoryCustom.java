package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepositoryCustom {
    Page<FindProductResponseDto> findProductByConditions(Long categoryId, List<Long> optionIds, String keyword, Pageable pageable);

    Page<FindProductResponseDto> findProductBySeller(Pageable pageable, Long sellerId);
}
