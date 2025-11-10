package com.space.munova.auth.service;

import com.space.munova.auth.dto.GenerateTokens;
import com.space.munova.member.entity.Member;

public interface TokenService {

    // 토큰 재발급
    GenerateTokens reissueToken(String refreshToken, String deviceId);

    // refreshToken 저장
    GenerateTokens saveRefreshToken(Member member, String deviceId);

    // refreshToken 저장
    // - 분산락 사용
    GenerateTokens saveRefreshTokenWithLock(Member member, String deviceId);

    // refreshToken 삭제
    void clearRefreshToken(Long memberId, String deviceId);

    // 모든 디바이스 refreshToken 삭제
    void clearAllDeviceRefreshToken(Long memberId);

    // SecurityContextHolder 비우기
    void clearSecurityContext();
}
