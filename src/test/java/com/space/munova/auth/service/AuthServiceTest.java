package com.space.munova.auth.service;

import com.space.munova.IntegrationTestBase;
import com.space.munova.auth.dto.*;
import com.space.munova.auth.exception.AuthException;
import com.space.munova.auth.repository.RefreshTokenRedisRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.security.jwt.JwtAuthenticationToken;
import com.space.munova.security.jwt.JwtHelper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Autowired
    private JwtHelper jwtHelper;

    private static final String USER_NAME = "testuser";
    private static final String USER_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = SignupRequest.of(USER_NAME, USER_PASSWORD, "");

        // when
        SignupResponse result = authService.signup(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(USER_NAME);
        assertThat(memberRepository.existsByUsername(USER_NAME)).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 사용자명")
    void signup_fail_duplicateUsername() {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));

        // when & then
        assertThatThrownBy(() -> authService.signup(SignupRequest.of(USER_NAME, "password456", "")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("로그인 성공")
    void signIn_success() {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));
        SignInRequest request = SignInRequest.of(USER_NAME, USER_PASSWORD);

        // when
        SignInGenerateToken result = authService.signIn(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();

        // Redis에 refreshToken 저장 확인
        Member member = memberRepository.findByUsername(USER_NAME).orElseThrow();
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNotNull();
        assertThat(storedRefreshToken).isEqualTo(result.refreshToken());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void signIn_fail_invalidPassword() {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));
        SignInRequest request = SignInRequest.of(USER_NAME, "wrongpassword");

        // when & then
        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void signOut_success() {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));
        SignInGenerateToken signInResult = authService.signIn(SignInRequest.of(USER_NAME, USER_PASSWORD));

        // SecurityContext 설정
        Member member = memberRepository.findByUsername(USER_NAME).orElseThrow();
        Claims claims = jwtHelper.getClaimsFromToken(signInResult.accessToken());
        JwtAuthenticationToken authentication = JwtAuthenticationToken.afterOf(member.getId(), member.getRole(), claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        authService.signOut();

        // then
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNull();
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueToken_success() throws InterruptedException {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));
        SignInGenerateToken signInResult = authService.signIn(SignInRequest.of(USER_NAME, USER_PASSWORD));

        String refreshToken = signInResult.refreshToken();

        // 시간 차이를 위해 잠시 대기
        Thread.sleep(1000);

        // when
        GenerateTokens result = tokenService.reissueToken(refreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        // accessToken이 재발급되었으므로 다를 것으로 예상
        assertThat(result.accessToken()).isNotEqualTo(signInResult.accessToken());

        // 새로운 refreshToken이 Redis에 저장되었는지 확인
        Member member = memberRepository.findByUsername(USER_NAME).orElseThrow();
        String newStoredRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(newStoredRefreshToken).isNotNull();
        assertThat(newStoredRefreshToken).isNotEqualTo(refreshToken);
        assertThat(newStoredRefreshToken).isEqualTo(result.refreshToken());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - null refreshToken")
    void reissueToken_fail_nullRefreshToken() {
        // when & then
        assertThatThrownBy(() -> tokenService.reissueToken(null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰과 불일치")
    void reissueToken_fail_mismatchedRefreshToken() throws InterruptedException {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));
        SignInGenerateToken signInResult = authService.signIn(SignInRequest.of(USER_NAME, USER_PASSWORD));

        Member member = memberRepository.findByUsername(USER_NAME).orElseThrow();

        // 시간 차이를 두어 다른 토큰 생성
        Thread.sleep(1000);
        String invalidRefreshToken = jwtHelper.generateRefreshToken(member.getId());

        // when & then
        assertThatThrownBy(() -> tokenService.reissueToken(invalidRefreshToken))
                .isInstanceOf(AuthException.class);

        // Redis에 원래 토큰이 그대로 있는지 확인 (삭제되지 않음)
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNotNull();
        assertThat(storedRefreshToken).isEqualTo(signInResult.refreshToken());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 refreshToken")
    void reissueToken_fail_expiredRefreshToken() {
        // given
        authService.signup(SignupRequest.of(USER_NAME, USER_PASSWORD, ""));
        Member member = memberRepository.findByUsername(USER_NAME).orElseThrow();

        // 만료된 토큰 생성 (만료시간 -1초)
        String expiredToken = createExpiredToken(member.getId());

        // when & then
        assertThatThrownBy(() -> tokenService.reissueToken(expiredToken))
                .isInstanceOf(AuthException.class);
    }

    private String createExpiredToken(Long memberId) {
        long now = System.currentTimeMillis();
        return io.jsonwebtoken.Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(new java.util.Date(now - 2000))
                .expiration(new java.util.Date(now - 1000))
                .signWith(getSecretKey())
                .compact();
    }

    private javax.crypto.SecretKey getSecretKey() {
        // application.yml의 jwt.secret과 동일한 값 사용
        String secret = "test-super-long-string-secret-at-least-256-bits-long-for-hmac-sha-256";
        byte[] bytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(bytes);
    }
}
