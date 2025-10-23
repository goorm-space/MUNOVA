package com.space.munova.security.jwt;

import com.space.munova.member.dto.MemberRole;
import com.space.munova.security.exception.CustomAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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

    @Value("${jwt.invalid-token-code}")
    private String invalidTokenCode;

    @Value("${jwt.expired-token-code}")
    private String expiredTokenCode;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * accessToken 생성
     */
    public String generateAccessToken(Long memberId, MemberRole role) {
        long now = System.currentTimeMillis();
        Date accessTokenExpiration = new Date(now + accessExpiration * 1000);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("authorities", role)
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
            throw new CustomAuthenticationException("잘못된 JWT 서명입니다.", invalidTokenCode);
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException("만료된 JWT 토큰입니다.", expiredTokenCode);
        } catch (UnsupportedJwtException e) {
            throw new CustomAuthenticationException("지원되지 않는 JWT 토큰입니다.", invalidTokenCode);
        } catch (IllegalArgumentException e) {
            throw new CustomAuthenticationException("JWT 토큰이 잘못되었습니다.", invalidTokenCode);
        }
    }

    /**
     * memberId 반환
     */
    public static Long getMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return (Long) jwtAuth.getPrincipal();
        }
        return null;
    }

    /**
     * (제너릭 타입) 정보 가져오기
     */
    public <T> T getClaims(String token, Function<Claims, T> func) {
        Claims claims = getClaimsFromToken(token);
        return func.apply(claims);
    }

    /**
     * Token Claims 가져오기
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
