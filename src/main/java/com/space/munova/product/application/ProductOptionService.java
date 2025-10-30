package com.space.munova.product.application;

import com.space.munova.product.application.dto.ProductOptionResponseDto;
import com.space.munova.product.domain.Repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductOptionService {

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
