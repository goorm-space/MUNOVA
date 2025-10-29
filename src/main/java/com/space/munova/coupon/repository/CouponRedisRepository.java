package com.space.munova.coupon.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class CouponRedisRepository {

    private static final String COUPON_STOCK_PREFIX = "coupon:stock:";
    private final RedisTemplate<String, Object> redisTemplate;

    public void saveQuantity(Long couponDetailId, Long quantity, Long expireTime) {
        String key = COUPON_STOCK_PREFIX + couponDetailId;
        redisTemplate.opsForValue().set(
                key,
                quantity,
                expireTime,
                TimeUnit.MILLISECONDS
        );
    }

    public Long decreaseQuantity(Long couponDetailId) {
        String key = COUPON_STOCK_PREFIX + couponDetailId;
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long findBy(Long couponDetailId) {
        String key = COUPON_STOCK_PREFIX + couponDetailId;
        Object amount = redisTemplate.opsForValue().get(key);
        return amount != null ? Long.parseLong(amount.toString()) : null;
    }

    public List<Object> findAllByIds(List<Long> couponDetailIds) {
        if (couponDetailIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> keys = couponDetailIds.stream()
                .map(id -> COUPON_STOCK_PREFIX + id)
                .toList();

        return redisTemplate.opsForValue().multiGet(keys);
    }

    public void delete(Long memberId) {
        String key = COUPON_STOCK_PREFIX + memberId;
        redisTemplate.delete(key);
    }
}
