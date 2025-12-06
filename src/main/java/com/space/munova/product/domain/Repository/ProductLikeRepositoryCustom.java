package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductLikeRepositoryCustom {
    Page<FindProductResponseDto> findLikeProductByMemberId(Pageable pageable, Long memberId);
}
