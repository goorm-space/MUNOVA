package com.space.munova.auth.controller;

import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignInResponse;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.auth.dto.SignupResponse;
import com.space.munova.auth.service.AuthService;
import com.space.munova.core.config.ResponseApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseApi<SignupResponse> signup(@Valid @RequestBody SignupRequest signUpRequest) {
        SignupResponse signup = authService.signup(signUpRequest);
        return ResponseApi.ok(signup);
    }

    /**
     * 로그인
     */
    @PostMapping("/signin")
    public ResponseApi<SignInResponse> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        SignInResponse signIn = authService.signIn(signInRequest);
        return ResponseApi.ok(signIn);
    }
}
