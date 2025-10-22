package com.space.munova.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private final RedisTemplate<String, Object> restTemplate;

    public void save(Long memberId, String refreshToken, Long refreshExpireTime) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        restTemplate.opsForValue().set(
                key,
                refreshToken,
                refreshExpireTime,
                TimeUnit.MILLISECONDS
        );
    }

    public String findBy(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        Object refreshToken = restTemplate.opsForValue().get(key);
        return refreshToken != null ? refreshToken.toString() : null;
    }

    public void delete(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        restTemplate.delete(key);
    }

}
