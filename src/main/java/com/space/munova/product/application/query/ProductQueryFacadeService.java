package com.space.munova.product.application.query;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.command.exception.ProductException;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductSearchRequestDto;
import com.space.munova.product.application.query.strategy.SearchStrategy;
import com.space.munova.product.infra.elasticsearch.query.ProductEsQueryRepoCustom;
import com.space.munova.product.infra.mongo.query.ProductMongoQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductQueryFacadeService {
    private final List<SearchStrategy> searchStrategies;

    /*
    * - 키워드별 상품 전체 조회
    * -> 키워드 o -> EsQueryStrategy
    * -> 키워드 x -> MongoQueryStrategy
    * */
    public PagingResponse<FindProductResponseDto> findProducts(
            ProductSearchRequestDto productSearchRequestDto,
            Pageable pageable) {


        Page<FindProductResponseDto> values = searchStrategies.stream()
                .filter(strategy -> strategy.supports(productSearchRequestDto))
                .findFirst()
                .map(strategy -> strategy.search(productSearchRequestDto, pageable))
                .orElseThrow(() -> ProductException.badRequestException("잘못된 요청입니다."));


        return PagingResponse.from(values);
    }


}
