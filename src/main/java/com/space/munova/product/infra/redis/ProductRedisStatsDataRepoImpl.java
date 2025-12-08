package com.space.munova.product.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProductRedisStatsDataRepoImpl implements ProductRedisStatsDataRepo {

   private final RedisTemplate redisTemplate;

   /// 버전별 상품 통계 관리위한 레디스 키 (맵으로 관리)
   private static final String PRODUCT_STATS = "product-stats";

   /// 루아스크립트로 원자적으로 연산처리


    /// 증가
    @Override
    public Long incrementSalesCount(Long productId, int count) {
       return incrementField(productId, "salesCount", count);
    }

    @Override
    public Long incrementLikeCount(Long productId, int count) {
        return incrementField(productId, "likeCount", count);
    }

    @Override
    public Long incrementViewCount(Long productId, int count) {
        return incrementField(productId, "viewCount", count);
    }

    ///  감소
    @Override
    public Long decrementLikeCount(Long productId, int count) {

        return decrementField(productId, "likeCount", count);
    }


    /// ============================== 내부 로직 =================================///

    /// 증가메서드.
    private Long incrementField(Long productId, String field, int count) {
        String key = PRODUCT_STATS + productId;
        return redisTemplate.opsForHash().increment(key, field, count);
    }

    /// 감소메서드.
    private Long decrementField(Long productId, String field, int count) {
        String key = PRODUCT_STATS + productId;
        return redisTemplate.opsForHash().increment(key, field, -count);
    }

}
