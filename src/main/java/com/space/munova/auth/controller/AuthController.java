package com.space.munova.auth.controller;

import com.space.munova.auth.dto.*;
import com.space.munova.auth.service.AuthService;
import com.space.munova.auth.service.TokenService;
import com.space.munova.core.config.ResponseApi;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.space.munova.core.config.StaticVariables.DEVICE_ID_HEADER_PREFIX;
import static com.space.munova.core.config.StaticVariables.REFRESH_TOKEN_COOKIE_KEY;


@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final JwtHelper jwtHelper;

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
    public ResponseApi<SignInResponse> signIn(
            @Valid @RequestBody SignInRequest signInRequest,
            HttpServletResponse response,
            @RequestHeader(value = DEVICE_ID_HEADER_PREFIX) String deviceId
    ) {
        SignInGenerateToken signInGenerateToken = authService.signIn(signInRequest, deviceId);
        // refreshToken 쿠키 저장
        jwtHelper.saveRefreshTokenToCookie(response, signInGenerateToken.refreshToken());
        response.setHeader(DEVICE_ID_HEADER_PREFIX, deviceId);

        SignInResponse signInResponse = SignInResponse.of(
                signInGenerateToken.memberId(),
                signInRequest.username(),
                signInGenerateToken.accessToken(),
                signInGenerateToken.refreshToken(),
                signInGenerateToken.role()

        );
        return ResponseApi.ok(signInResponse);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/api/auth/signout")
    public ResponseApi<Void> signOut(
            HttpServletResponse response,
            @RequestHeader(value = DEVICE_ID_HEADER_PREFIX) String deviceId
    ) {
        authService.signOut(deviceId);
        // 쿠키에서 refreshToken 삭제
        jwtHelper.deleteRefreshTokenFromCookie(response);
        return ResponseApi.ok();
    }

    /**
     * 토큰 재발급
     */
    @PostMapping("/auth/reissue")
    public ResponseApi<TokenReissueResponse> reissueToken(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_KEY, required = false) String refreshToken,
            @RequestHeader(value = DEVICE_ID_HEADER_PREFIX) String deviceId,
            HttpServletResponse response
    ) {
        GenerateTokens generateTokens = tokenService.reissueToken(refreshToken, deviceId);
        // refreshToken 쿠키 저장
        jwtHelper.saveRefreshTokenToCookie(response, generateTokens.refreshToken());

        TokenReissueResponse tokenReissueResponse =
                TokenReissueResponse.of(generateTokens.accessToken());
        return ResponseApi.ok(tokenReissueResponse);
    }
}
