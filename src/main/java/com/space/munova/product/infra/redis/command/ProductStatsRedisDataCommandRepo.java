package com.space.munova.product.infra.redis.command;

public interface ProductStatsRedisDataCommandRepo {

    void resetAllocatedProductId();

    Long findAllocatedProductId();

    Long incrementSalesCount(Long productId, int count);

    Long incrementLikeCount(Long productId,int count);

    Long incrementViewCount(Long productId,int count);

    Long decrementLikeCount(Long productId,int count);


}
