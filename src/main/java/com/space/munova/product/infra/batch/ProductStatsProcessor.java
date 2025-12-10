package com.space.munova.product.infra.batch;

import com.space.munova.product.infra.batch.dto.ProductStatsSyncDto;
import com.space.munova.product.infra.redis.query.ProductStatsRedisDataQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductStatsProcessor implements ItemProcessor<Long, ProductStatsSyncDto> {

    private final ProductStatsRedisDataQueryRepo productStatsRedisDataQueryRepo;


    @Override
    public ProductStatsSyncDto process(Long productId) throws Exception {

        Integer likeCount = productStatsRedisDataQueryRepo.findProductLikeCount(productId);
        if (likeCount == null) likeCount = 0;

        Integer salesCount = productStatsRedisDataQueryRepo.findProductSalesCount(productId);
        if (salesCount == null) salesCount = 0;

        Integer viewCount = productStatsRedisDataQueryRepo.findProductViewCount(productId);
        if (viewCount == null) viewCount = 0;


        return ProductStatsSyncDto.builder()
                .likeCount(likeCount)
                .salesCount(salesCount)
                .viewCount(viewCount)
                .productId(productId)
                .build();
    }
}
