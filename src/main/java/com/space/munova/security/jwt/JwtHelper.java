package com.space.munova.security.jwt;

import com.space.munova.member.dto.MemberRole;
import com.space.munova.security.exception.CustomAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtHelper {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey secretKey;

    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String EXPIRED_TOKEN = "EXPIRED_TOKEN";
    public static final String ROLE_CLAIM_KEY = "authorities";
    public static final String NAME_CLAIM_KEY = "username";
    public static final String REFRESH_TOKEN_COOKIE_KEY = "refresh-token";

    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * accessToken 생성
     */
    public String generateAccessToken(Long memberId, String userName, MemberRole role) {
        long now = System.currentTimeMillis();
        Date accessTokenExpiration = new Date(now + accessExpiration * 1000);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim(ROLE_CLAIM_KEY, role)
                .claim(NAME_CLAIM_KEY, userName)
                .issuedAt(new Date(now))
                .expiration(accessTokenExpiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * refreshToken 생성
     */
    public String generateRefreshToken(Long memberId) {
        long now = System.currentTimeMillis();
        Date refreshTokenExpiration = new Date(now + refreshExpiration * 1000);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(new Date(now))
                .expiration(refreshTokenExpiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검사
     */
    public void validateJwt(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (SecurityException | MalformedJwtException e) {
            throw new CustomAuthenticationException("잘못된 JWT 서명입니다.", INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException("만료된 JWT 토큰입니다.", EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomAuthenticationException("지원되지 않는 JWT 토큰입니다.", INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomAuthenticationException("JWT 토큰이 잘못되었습니다.", INVALID_TOKEN);
        }
    }

    /**
     * refreshToken을 쿠키에 저장
     */
    public void saveRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_KEY, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(refreshExpiration));
        response.addCookie(cookie);
    }

    /**
     * 쿠키에서 refreshToken 삭제
     */
    public void deleteRefreshTokenFromCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_KEY, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * memberId 반환
     */
    public static Long getMemberId() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return (Long) jwtAuth.getPrincipal();
        }
        return null;
    }

    /**
     * userName 반환
     */
    public static String getMemberName() {
        Object username = getClaims(NAME_CLAIM_KEY);
        return username != null ? username.toString() : null;
    }

    /**
     * memberRole 반환
     */
    public static MemberRole getMemberRole() {
        Object authorities = getClaims(ROLE_CLAIM_KEY);
        if (authorities == null) {
            return MemberRole.USER;
        }
        return MemberRole.valueOf(authorities.toString());
    }

    /**
     * (제너릭 타입) claims value 가져오기
     */
    private static Object getClaims(String key) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Claims claims = jwtAuth.getClaims();
            return claims.get(key);
        }
        return null;
    }

    /**
     * (제너릭 타입) token을 이용해 claims value 가져오기
     */
    public <T> T getClaims(String token, Function<Claims, T> func) {
        Claims claims = getClaimsFromToken(token);
        return func.apply(claims);
    }

    /**
     * (제너릭 타입) claims을 이용해 claims value 가져오기
     */
    public <T> T getClaims(Claims claims, Function<Claims, T> func) {
        return func.apply(claims);
    }

    /**
     * Token Claims 가져오기
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication;
    }

}
