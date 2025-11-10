package com.space.munova.auth.service;

import com.space.munova.auth.dto.GenerateTokens;
import com.space.munova.auth.exception.AuthException;
import com.space.munova.auth.repository.RefreshTokenRedisRepository;
import com.space.munova.core.annotation.RedisDistributeLock;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.security.jwt.JwtHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenServiceImpl implements TokenService {

    private final JwtHelper jwtHelper;
    private final MemberRepository memberRepository;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    // 토큰 재발급
    @Override
    @RedisDistributeLock(key = "#refreshToken + ':' + #deviceId")
    public GenerateTokens reissueToken(String refreshToken, String deviceId) {
        // refreshToken 검증
        Claims claims = validateRefreshToken(refreshToken);

        // redis 비교
        Long memberId = Long.parseLong(claims.getSubject());
        refreshTokenRedisRepository.validateTokenById(memberId, deviceId, refreshToken);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::invalidMemberException);

        // 새 토큰 생성 및 저장
        GenerateTokens generateTokens = saveRefreshToken(member, deviceId);
        log.info("토큰 재발급 성공: {}", member.getUsername());
        return generateTokens;
    }

    // refreshToken 저장
    @Override
    public GenerateTokens saveRefreshToken(Member member, String deviceId) {
        return getGenerateTokens(member, deviceId);
    }

    // refreshToken 저장
    // - 분산락 사용
    @Override
    @RedisDistributeLock(key = "#member.getId() + ':' + #deviceId")
    public GenerateTokens saveRefreshTokenWithLock(Member member, String deviceId) {
        return getGenerateTokens(member, deviceId);
    }

    // refreshToken 저장
    private GenerateTokens getGenerateTokens(Member member, String deviceId) {
        String accessToken = jwtHelper.generateAccessToken(member.getId(), member.getUsername(), member.getRole());
        String refreshToken = jwtHelper.generateRefreshToken(member.getId());
        long expireTime = jwtHelper.getClaims(refreshToken, Claims::getExpiration).getTime();

        refreshTokenRedisRepository.save(member.getId(), refreshToken, expireTime, deviceId);

        return GenerateTokens.of(accessToken, refreshToken);
    }

    // refreshToken 삭제
    @Override
    public void clearRefreshToken(Long memberId, String deviceId) {
        // Redis에서 refreshToken 삭제
        refreshTokenRedisRepository.delete(memberId, deviceId);
    }

    // 모든 디바이스 refreshToken 삭제
    @Override
    public void clearAllDeviceRefreshToken(Long memberId) {
        refreshTokenRedisRepository.deleteAllDevices(memberId);
    }

    @Override
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // refreshToken 검증
    private Claims validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw AuthException.invalidTokenException();
        }
        try {
            return jwtHelper.getClaimsFromToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw AuthException.invalidTokenException();
        }
    }
}
