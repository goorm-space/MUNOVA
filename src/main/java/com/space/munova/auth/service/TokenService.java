package com.space.munova.auth.service;

import com.space.munova.auth.dto.GenerateTokens;
import com.space.munova.member.entity.Member;

public interface TokenService {

    // 토큰 재발급
    GenerateTokens reissueToken(String refreshToken);

    // refreshToken 저장
    GenerateTokens saveRefreshToken(Member member);

    // refreshToken 삭제
    void clearRefreshToken(Long memberId);

    // SecurityContextHolder 비우기
    void clearSecurityContext();
}
