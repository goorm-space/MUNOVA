package com.space.munova.product.infra.redis;

import java.util.Map;

public interface ProductRedisStatsDataRepo {

    Long incrementSalesCount(Long productId, int count);

    Long incrementLikeCount(Long productId,int count);

    Long incrementViewCount(Long productId,int count);

    Long decrementLikeCount(Long productId,int count);

}
