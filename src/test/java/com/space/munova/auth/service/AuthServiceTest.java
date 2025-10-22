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
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Autowired
    private JwtHelper jwtHelper;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
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
        SignupRequest request = new SignupRequest("testuser", "password123");

        // when
        SignupResponse result = authService.signup(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(memberRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 사용자명")
    void signup_fail_duplicateUsername() {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));

        // when & then
        assertThatThrownBy(() -> authService.signup(new SignupRequest("testuser", "password456")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("로그인 성공")
    void signIn_success() {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));
        SignInRequest request = new SignInRequest("testuser", "password123");

        // when
        SignInResponse result = authService.signIn(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();

        // Redis에 refreshToken 저장 확인
        Member member = memberRepository.findByUsername("testuser").orElseThrow();
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNotNull();

        // 쿠키에 refreshToken 저장 확인
        Cookie cookie = response.getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void signIn_fail_invalidPassword() {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));
        SignInRequest request = new SignInRequest("testuser", "wrongpassword");

        // when & then
        assertThatThrownBy(() -> authService.signIn(request, response))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void signOut_success() {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));
        SignInResponse signInResponse = authService.signIn(new SignInRequest("testuser", "password123"), response);

        // SecurityContext 설정
        Member member = memberRepository.findByUsername("testuser").orElseThrow();
        Claims claims = jwtHelper.getClaimsFromToken(signInResponse.accessToken());
        JwtAuthenticationToken authentication = JwtAuthenticationToken.afterOf(member.getId(), member.getRole(), claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();

        // when
        authService.signOut(logoutResponse);

        // then
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNull();

        Cookie cookie = logoutResponse.getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isEqualTo(0);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueToken_success() throws InterruptedException {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));
        SignInResponse signInResponse = authService.signIn(new SignInRequest("testuser", "password123"), response);

        Cookie cookie = response.getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        String refreshToken = cookie.getValue();

        // 시간 차이를 위해 잠시 대기
        Thread.sleep(1000);

        MockHttpServletResponse reissueResponse = new MockHttpServletResponse();

        // when
        TokenReissueResponse result = authService.reissueToken(refreshToken, reissueResponse);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        // accessToken이 재발급되었으므로 다를 것으로 예상
        assertThat(result.accessToken()).isNotEqualTo(signInResponse.accessToken());

        // 새로운 refreshToken이 Redis에 저장되었는지 확인
        Member member = memberRepository.findByUsername("testuser").orElseThrow();
        String newStoredRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(newStoredRefreshToken).isNotNull();
        assertThat(newStoredRefreshToken).isNotEqualTo(refreshToken);

        // 새로운 refreshToken이 쿠키에 저장되었는지 확인
        Cookie newCookie = reissueResponse.getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isNotBlank();
        assertThat(newCookie.getValue()).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - null refreshToken")
    void reissueToken_fail_nullRefreshToken() {
        // when & then
        assertThatThrownBy(() -> authService.reissueToken(null, response))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰과 불일치")
    void reissueToken_fail_mismatchedRefreshToken() throws InterruptedException {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));
        authService.signIn(new SignInRequest("testuser", "password123"), response);

        Member member = memberRepository.findByUsername("testuser").orElseThrow();

        // 시간 차이를 두어 다른 토큰 생성
        Thread.sleep(1000);
        String invalidRefreshToken = jwtHelper.generateRefreshToken(member.getId());

        MockHttpServletResponse reissueResponse = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(invalidRefreshToken, reissueResponse))
                .isInstanceOf(AuthException.class);

        // Redis에서 토큰이 삭제되었는지 확인
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNull();
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 refreshToken")
    void reissueToken_fail_expiredRefreshToken() {
        // given
        authService.signup(new SignupRequest("testuser", "password123"));
        Member member = memberRepository.findByUsername("testuser").orElseThrow();

        // 만료된 토큰 생성 (만료시간 -1초)
        String expiredToken = createExpiredToken(member.getId());

        MockHttpServletResponse reissueResponse = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(expiredToken, reissueResponse))
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
