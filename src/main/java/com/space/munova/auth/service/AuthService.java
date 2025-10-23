package com.space.munova.auth.service;

import com.space.munova.auth.dto.*;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    // 회원가입
    SignupResponse signup(SignupRequest signupRequest);

    // 로그인
    SignInResponse signIn(SignInRequest signInRequest, HttpServletResponse response);

    // 로그아웃
    void signOut(HttpServletResponse response);

    // 토큰 재발급
    TokenReissueResponse reissueToken(String refreshToken, HttpServletResponse response);

}
