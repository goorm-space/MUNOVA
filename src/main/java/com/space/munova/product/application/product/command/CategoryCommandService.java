package com.space.munova.product.application.product.command;

import com.space.munova.product.application.product.command.exception.ProductException;
import com.space.munova.product.application.product.query.dto.ProductCategoryResponseDto;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryCommandService {
    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(ProductException::notFoundCategoryException);
    }

}
