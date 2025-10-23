package com.space.munova.security.jwt;

import com.space.munova.IntegrationTestBase;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.security.exception.CustomAuthenticationException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtHelperTest extends IntegrationTestBase {

    @Autowired
    private JwtHelper jwtHelper;

    private MockHttpServletResponse response;

    private static final String USER_NAME = "testuser";

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AccessToken 생성 성공")
    void generateAccessToken_success() {
        // given
        Long memberId = 1L;
        String username = USER_NAME;
        MemberRole role = MemberRole.USER;

        // when
        String accessToken = jwtHelper.generateAccessToken(memberId, username, role);

        // then
        assertThat(accessToken).isNotBlank();

        Claims claims = jwtHelper.getClaimsFromToken(accessToken);
        assertThat(claims.getSubject()).isEqualTo(memberId.toString());
        assertThat(claims.get(JwtHelper.NAME_CLAIM_KEY)).isEqualTo(username);
        assertThat(claims.get(JwtHelper.ROLE_CLAIM_KEY).toString()).isEqualTo(role.toString());
    }

    @Test
    @DisplayName("RefreshToken 생성 성공")
    void generateRefreshToken_success() {
        // given
        Long memberId = 1L;

        // when
        String refreshToken = jwtHelper.generateRefreshToken(memberId);

        // then
        assertThat(refreshToken).isNotBlank();

        Claims claims = jwtHelper.getClaimsFromToken(refreshToken);
        assertThat(claims.getSubject()).isEqualTo(memberId.toString());
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 성공")
    void validateJwt_success() {
        // given
        String accessToken = jwtHelper.generateAccessToken(1L, USER_NAME, MemberRole.USER);

        // when & then
        jwtHelper.validateJwt(accessToken);
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 실패 - 만료된 토큰")
    void validateJwt_fail_expiredToken() {
        // given
        String expiredToken = createExpiredToken(1L);

        // when & then
        assertThatThrownBy(() -> jwtHelper.validateJwt(expiredToken))
                .isInstanceOf(CustomAuthenticationException.class)
                .hasMessageContaining("만료된 JWT 토큰입니다.");
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 실패 - 잘못된 서명")
    void validateJwt_fail_invalidSignature() {
        // given
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.invalid";

        // when & then
        assertThatThrownBy(() -> jwtHelper.validateJwt(invalidToken))
                .isInstanceOf(Exception.class); // SignatureException이 먼저 발생
    }

    @Test
    @DisplayName("RefreshToken 쿠키 저장 성공")
    void saveRefreshTokenToCookie_success() {
        // given
        String refreshToken = jwtHelper.generateRefreshToken(1L);

        // when
        jwtHelper.saveRefreshTokenToCookie(response, refreshToken);

        // then
        Cookie cookie = response.getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(refreshToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isGreaterThan(0);
    }

    @Test
    @DisplayName("RefreshToken 쿠키 삭제 성공")
    void deleteRefreshTokenFromCookie_success() {
        // when
        jwtHelper.deleteRefreshTokenFromCookie(response);

        // then
        Cookie cookie = response.getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNull();
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    @DisplayName("Claims에서 값 추출 성공")
    void getClaims_success() {
        // given
        String accessToken = jwtHelper.generateAccessToken(1L, USER_NAME, MemberRole.USER);

        // when
        String subject = jwtHelper.getClaims(accessToken, Claims::getSubject);
        String username = jwtHelper.getClaims(accessToken, claims -> claims.get(JwtHelper.NAME_CLAIM_KEY, String.class));

        // then
        assertThat(subject).isEqualTo("1");
        assertThat(username).isEqualTo(USER_NAME);
    }

    @Test
    @DisplayName("SecurityContext에서 MemberId 가져오기 성공")
    void getMemberId_success() {
        // given
        Long memberId = 1L;
        String accessToken = jwtHelper.generateAccessToken(memberId, USER_NAME, MemberRole.USER);
        Claims claims = jwtHelper.getClaimsFromToken(accessToken);
        JwtAuthenticationToken authentication = JwtAuthenticationToken.afterOf(memberId, MemberRole.USER, claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Long result = JwtHelper.getMemberId();

        // then
        assertThat(result).isEqualTo(memberId);
    }

    @Test
    @DisplayName("SecurityContext에서 MemberName 가져오기 성공")
    void getMemberName_success() {
        // given
        String username = USER_NAME;
        String accessToken = jwtHelper.generateAccessToken(1L, username, MemberRole.USER);
        Claims claims = jwtHelper.getClaimsFromToken(accessToken);
        JwtAuthenticationToken authentication = JwtAuthenticationToken.afterOf(1L, MemberRole.USER, claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        String result = JwtHelper.getMemberName();

        // then
        assertThat(result).isEqualTo(username);
    }

    @Test
    @DisplayName("SecurityContext에서 MemberRole 가져오기 성공")
    void getMemberRole_success() {
        // given
        MemberRole role = MemberRole.ADMIN;
        String accessToken = jwtHelper.generateAccessToken(1L, USER_NAME, role);
        Claims claims = jwtHelper.getClaimsFromToken(accessToken);
        JwtAuthenticationToken authentication = JwtAuthenticationToken.afterOf(1L, role, claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        MemberRole result = JwtHelper.getMemberRole();

        // then
        assertThat(result).isEqualTo(role);
    }

    @Test
    @DisplayName("SecurityContext가 비어있을 때 MemberId는 null 반환")
    void getMemberId_null_whenSecurityContextEmpty() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Long result = JwtHelper.getMemberId();

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("SecurityContext가 비어있을 때 MemberName은 null 반환")
    void getMemberName_null_whenSecurityContextEmpty() {
        // given
        SecurityContextHolder.clearContext();

        // when
        String result = JwtHelper.getMemberName();

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("AccessToken과 RefreshToken의 만료시간이 다름")
    void token_expiration_different() throws InterruptedException {
        // given
        Long memberId = 1L;

        // when
        String accessToken = jwtHelper.generateAccessToken(memberId, USER_NAME, MemberRole.USER);
        Thread.sleep(10);
        String refreshToken = jwtHelper.generateRefreshToken(memberId);

        // then
        Claims accessClaims = jwtHelper.getClaimsFromToken(accessToken);
        Claims refreshClaims = jwtHelper.getClaimsFromToken(refreshToken);

        long accessExpiration = accessClaims.getExpiration().getTime() - accessClaims.getIssuedAt().getTime();
        long refreshExpiration = refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime();

        assertThat(accessExpiration).isLessThan(refreshExpiration);
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
