package com.space.munova.product.infra.redis.query;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductStatsRedisDataQueryRepoImpl implements ProductStatsRedisDataQueryRepo {

    private final RedisTemplate<String, Object> redisTemplate;

    /// 버전별 상품 통계 관리위한 레디스 키 (맵으로 관리)
    private static final String PRODUCT_STATS = "product-stats";


    @Override
    public Integer findProductSalesCount(Long productId) {
        String key = PRODUCT_STATS + productId;
        Object value = redisTemplate.opsForHash().get(key, "salesCount"); // 맵에서 라이크카운트 가져옴.

        if (value == null) {
            return 0; // 키가 없거나 필드가 없으면 0 반환
        }

        return Integer.valueOf(value.toString());
    }

    @Override
    public Integer findProductLikeCount(Long productId) {

        String key = PRODUCT_STATS + productId;
        Object value = redisTemplate.opsForHash().get(key, "likeCount"); // 맵에서 라이크카운트 가져옴.

        if (value == null) {
            return 0; // 키가 없거나 필드가 없으면 0 반환
        }

        return Integer.valueOf(value.toString());
    }

    @Override
    public Integer findProductViewCount(Long productId) {
        String key = PRODUCT_STATS + productId;
        Object value = redisTemplate.opsForHash().get(key, "viewCount"); // 맵에서 라이크카운트 가져옴.

        if (value == null) {
            return 0; // 키가 없거나 필드가 없으면 0 반환
        }

        return Integer.valueOf(value.toString());
    }
}
