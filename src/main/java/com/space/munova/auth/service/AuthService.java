package com.space.munova.auth.service;

import com.space.munova.auth.dto.SignInGenerateToken;
import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.auth.dto.SignupResponse;

public interface AuthService {

    // 회원가입
    SignupResponse signup(SignupRequest signupRequest);

    // 로그인
    SignInGenerateToken signIn(SignInRequest signInRequest, String deviceId);

    // 로그아웃
    void signOut(String deviceId, Long memberId);

    // 권한 확인
    void verifyAuthorization(Long actualOwnerId, Long currentMemberId);
}
