package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.dto.FindProductResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepositoryCustom {
    List<FindProductResponseDto> findProductByConditions(Long categoryId, List<Long> optionIds, String keyword, Pageable pageable);

    List<FindProductResponseDto> findProductBySeller(Pageable pageable, Long sellerId);
}
