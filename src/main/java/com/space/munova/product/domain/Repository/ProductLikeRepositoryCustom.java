package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.dto.FindProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductLikeRepositoryCustom {
    Page<FindProductResponseDto> findLikeProductByMemberId(Pageable pageable, Long memberId);
}
