package com.space.munova.product.application.cart.query;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.cart.query.dto.FindCartInfoResponseDto;
import com.space.munova.product.application.cart.query.dto.ProductInfoForCartDto;
import com.space.munova.product.domain.Repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartQueryService {

    private final CartRepository cartRepository;

    public PagingResponse<FindCartInfoResponseDto> findCartItemByMember(Pageable pageable, Long memberId) {

        ///  해당 페이지에 보여줄 상품디테일 아이디 (리밋 오프셋을통해 가져올 정보를 확인)
        Page<Long> detailIdsPage = cartRepository.findDistinctDetailIdsByMemberId(memberId, pageable);

        if (detailIdsPage.isEmpty()) {
            return PagingResponse.from(Page.empty());
        }
        List<Long> detailIds = detailIdsPage.getContent();

        /// 가져올 아이디 정보들을  가지고 다시 조회. -> 리밋 오프셋으로 상품정보가 한 페이지 안에 하나의 상품정보가 다 못담겨질수있기때문에
        ///  detailIdsPage 과 나눠서 다시조회.
        List<ProductInfoForCartDto> productInfoList = cartRepository.findCartItemInfoByDetailIds(detailIds);

        Map<Long, List<ProductInfoForCartDto>> groupedByDetail =
                productInfoList.stream()
                        .collect(Collectors.groupingBy(
                                ProductInfoForCartDto::detailId,
                                LinkedHashMap::new, // 순서 보장
                                Collectors.toList()
                        ));

        List<FindCartInfoResponseDto> content = detailIds.stream()
                .map(groupedByDetail::get)
                .map(FindCartInfoResponseDto::from)
                .collect(Collectors.toList());

        Page<FindCartInfoResponseDto> resultPage =
                new PageImpl<>(content, detailIdsPage.getPageable(), detailIdsPage.getTotalElements());

        return PagingResponse.from(resultPage);
    }

}
