package com.space.munova.auth.controller;

import com.space.munova.auth.dto.*;
import com.space.munova.auth.service.AuthService;
import com.space.munova.core.config.ResponseApi;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.space.munova.security.jwt.JwtHelper.REFRESH_TOKEN_COOKIE_KEY;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     */
    @PostMapping("/auth/signup")
    public ResponseApi<SignupResponse> signup(@Valid @RequestBody SignupRequest signUpRequest) {
        SignupResponse signup = authService.signup(signUpRequest);
        return ResponseApi.ok(signup);
    }

    /**
     * 로그인
     */
    @PostMapping("/auth/signin")
    public ResponseApi<SignInResponse> signIn(@Valid @RequestBody SignInRequest signInRequest, HttpServletResponse response) {
        SignInResponse signIn = authService.signIn(signInRequest, response);
        return ResponseApi.ok(signIn);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/api/auth/signout")
    public ResponseApi<Void> signOut(HttpServletResponse response) {
        authService.signOut(response);
        return ResponseApi.ok();
    }

    /**
     * 토큰 재발급
     */
    @PostMapping("/auth/reissue")
    public ResponseApi<TokenReissueResponse> reissueToken(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_KEY, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        TokenReissueResponse tokenReissueResponse = authService.reissueToken(refreshToken, response);
        return ResponseApi.ok(tokenReissueResponse);
    }
}
