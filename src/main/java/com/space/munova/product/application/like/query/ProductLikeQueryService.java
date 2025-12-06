package com.space.munova.product.application.like.query;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.domain.Repository.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductLikeQueryService {

    private final ProductLikeRepository productLikeRepository;

    public PagingResponse<FindProductResponseDto> findLikeProducts(Pageable pageable, Long memberId) {

        Page<FindProductResponseDto> likeProductList = productLikeRepository.findLikeProductByMemberId(pageable, memberId);
        return PagingResponse.from(likeProductList);
    }
}
