package com.space.munova.product.domain.Repository;


import com.space.munova.product.application.dto.cart.ProductInfoForCartDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CartRepositoryCustom {


    List<ProductInfoForCartDto> findCartItemInfoByDetailIds(List<Long> detailIds);

    Page<Long> findDistinctDetailIdsByMemberId(Long memberId, Pageable pageable);
}

