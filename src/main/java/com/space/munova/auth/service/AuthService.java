package com.space.munova.auth.service;

import com.space.munova.auth.dto.SignInGenerateToken;
import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignupRequest;

public interface AuthService {

    // 회원가입
    void signup(SignupRequest signupRequest);

    // 로그인
    SignInGenerateToken signIn(SignInRequest signInRequest, String deviceId);

    // 로그아웃
    void signOut(String deviceId, Long memberId);

}
