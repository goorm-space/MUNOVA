package com.space.munova.product.application.product.query;


import com.space.munova.product.application.product.query.dto.ProductCategoryResponseDto;
import com.space.munova.product.domain.enums.ProductCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// 읽기 전용 카테고리 서비스
@Service
@Transactional(readOnly = true)
public class ProductQueryCategoryService {

    /// 모든 카테고리 조회 메서드
    public List<ProductCategoryResponseDto> findProductCategories() {
        return ProductCategory.findCategoryInfoList();
    }
}
