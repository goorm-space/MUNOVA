package com.space.munova.product.infra.mongo.query;


import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductCursorDto;
import com.space.munova.product.domain.enums.SortFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface ProductMongoQueryRepoCustom {
    Page<FindProductResponseDto> findByConditions(SortFlag sortFlag,
                                                  ProductCursorDto productCursorDto,
                                                  Long categoryId,
                                                  List<Long> optionIds,
                                                  Pageable pageable);

}
