package com.space.munova.auth.service;

import com.space.munova.auth.dto.SignInGenerateToken;
import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.auth.dto.SignupResponse;

public interface AuthService {

    // 회원가입
    SignupResponse signup(SignupRequest signupRequest);

    // 로그인
    SignInGenerateToken signIn(SignInRequest signInRequest);

    // 로그아웃
    void signOut();

}
