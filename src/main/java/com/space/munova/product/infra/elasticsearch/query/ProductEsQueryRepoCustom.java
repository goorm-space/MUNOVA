package com.space.munova.product.infra.elasticsearch.query;

import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductCursorDto;
import com.space.munova.product.domain.enums.SortFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductEsQueryRepoCustom {

    Page<FindProductResponseDto> search(SortFlag sortFlag,
                                        ProductCursorDto productCursorDto,
                                        Long categoryId,
                                        List<Long> optionIds,
                                        String keyword,
                                        Pageable pageable);
}
