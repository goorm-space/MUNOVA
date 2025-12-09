package com.space.munova.product.infra.redis.query.adapter;

import com.space.munova.product.application.product.query.port.ProductRedisQueryPort;
import com.space.munova.product.infra.redis.query.ProductStatsRedisDataQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductRedisQueryAdapter implements ProductRedisQueryPort {

    private final ProductStatsRedisDataQueryRepo productStatsRedisDataQueryRepo;

    @Override
    public Integer findProductLikeCount(Long productId) {
        return productStatsRedisDataQueryRepo.findProductLikeCount(productId);
    }

    @Override
    public Integer findProductViewCount(Long productId) {
        return productStatsRedisDataQueryRepo.findProductViewCount(productId);
    }
}
