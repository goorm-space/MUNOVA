package com.space.munova.auth.repository;

import com.space.munova.auth.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class RefreshTokenRedisRepository {

    private final String refreshTokenPrefix;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    public RefreshTokenRedisRepository(
            @Value("${jwt.refresh-token-prefix:RT:}") String refreshTokenPrefix,
            RedisTemplate<String, Object> redisTemplate,
            RedissonClient redissonClient
    ) {
        this.refreshTokenPrefix = refreshTokenPrefix;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    public void save(Long memberId, String refreshToken, Long refreshExpireTime, String deviceId) {
        String key = makeKey(memberId, deviceId);
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                refreshExpireTime,
                TimeUnit.MILLISECONDS
        );
    }

    public void validateTokenById(Long memberId, String deviceId, String refreshToken) {
        String key = makeKey(memberId, deviceId);
        Object storedRefreshToken = redisTemplate.opsForValue().get(key);

        if (storedRefreshToken == null) {
            log.warn("만료된 refreshToken 요청: memberId={}", memberId);
            throw AuthException.tokenExpiredException();
        } else if (!storedRefreshToken.toString().equals(refreshToken)) {
            log.error("refreshToken 불일치: memberId={}", memberId);
            throw AuthException.invalidTokenException();
        }
    }

    public void delete(Long memberId, String deviceId) {
        String key = makeKey(memberId, deviceId);
        redisTemplate.delete(key);
    }

    // 특정 사용자의 모든 디바이스 로그아웃
    public void deleteAllDevices(Long memberId) {
        RKeys rkeys = redissonClient.getKeys();
        String pattern = refreshTokenPrefix + memberId + ":*";
        rkeys.deleteByPatternAsync(pattern);
    }

    private String makeKey(Long memberId, String deviceId) {
        return refreshTokenPrefix + memberId + ":" + deviceId;
    }
}
