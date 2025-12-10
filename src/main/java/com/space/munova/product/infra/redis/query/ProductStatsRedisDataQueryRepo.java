package com.space.munova.product.infra.redis.query;

public interface ProductStatsRedisDataQueryRepo {

    Integer findProductSalesCount(Long productId);

    Integer findProductLikeCount(Long productId);

    Integer findProductViewCount(Long productId);
}
