package com.space.munova.product.application.product.query;

import com.space.munova.product.application.product.query.dto.ProductOptionResponseDto;
import com.space.munova.product.domain.Repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// mysql 읽기전용 옵션 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryOptionService {

    private final ProductOptionRepository productOptionRepository;

    public List<ProductOptionResponseDto> findOptions() {
        return productOptionRepository
                .findAll()
                .stream()
                .map(op -> new ProductOptionResponseDto(
                        op.getId(),
                        op.getOptionType().name(),
                        op.getOptionName()
                ))
                .toList();
    }
}
