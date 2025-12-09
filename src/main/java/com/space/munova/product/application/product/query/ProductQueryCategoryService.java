package com.space.munova.product.application.product.query;


import com.space.munova.product.application.product.query.dto.ProductCategoryResponseDto;
import com.space.munova.product.domain.Repository.CategoryRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// 읽기 전용 카테고리 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryCategoryService {

    private final CategoryRepository categoryRepository;

    /// 모든 카테고리 조회 메서드
    public List<ProductCategoryResponseDto> findProductCategories() {
        return ProductCategory.findCategoryInfoList();
    }

    ///  카테고리 디비 조회쿼리 -> 사용x (서버내 이넘으로 캐싱된 카테고리 사용)
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
