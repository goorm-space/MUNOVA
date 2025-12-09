package com.space.munova.product.infra.redis.command.adapter;

import com.space.munova.product.application.like.command.port.ProductLikeCommandPort;
import com.space.munova.product.application.product.command.port.ProductRedisCommandPort;
import com.space.munova.product.infra.redis.command.ProductStatsRedisDataCommandRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRedisCommandAdapter implements ProductRedisCommandPort, ProductLikeCommandPort {

    private final ProductStatsRedisDataCommandRepo productStatsRedisDataCommandRepo;

    @Override
    public Long like(Long productId, int input) {
        return productStatsRedisDataCommandRepo.incrementLikeCount(productId, input);
    }

    @Override
    public Long dislike(Long productId, int input) {
        return productStatsRedisDataCommandRepo.decrementLikeCount(productId, input);
    }
    @Override
    public Long updateViewCount(Long productId, int input) {
        return productStatsRedisDataCommandRepo.incrementViewCount(productId, input);
    }
}
