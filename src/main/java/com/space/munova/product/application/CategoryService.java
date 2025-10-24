package com.space.munova.product.application;

import com.space.munova.product.application.dto.ProductCategoryResponseDto;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(ProductException::notFoundCategoryException);
    }

    public List<ProductCategoryResponseDto> findAllProductCategories() {
        return categoryRepository
                    .findAll()
                    .stream()
                    .map(c -> new ProductCategoryResponseDto(
                            c.getId(),
                            c.getCategoryType().name(),
                            (c.getRefCategory() != null) ? c.getRefCategory().getId() : null,
                            c.getLevel()))
                    .toList();
    }
}
