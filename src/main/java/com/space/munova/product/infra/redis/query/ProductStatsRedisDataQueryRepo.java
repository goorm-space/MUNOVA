package com.space.munova.product.infra.redis.query;

public interface ProductStatsRedisDataQueryRepo {

    Integer findProductLikeCount(Long productId);

    Integer findProductViewCount(Long productId);
}
