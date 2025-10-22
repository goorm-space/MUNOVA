package com.space.munova.auth.service;

import com.space.munova.auth.dto.*;
import com.space.munova.auth.exception.AuthException;
import com.space.munova.auth.repository.RefreshTokenRedisRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.security.exception.CustomAuthenticationException;
import com.space.munova.security.jwt.JwtHelper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final JwtHelper jwtHelper;

    /**
     * 회원가입
     */
    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {
        // 중복 사용자 확인
        String username = signupRequest.username();
        if (memberRepository.existsByUsername(username)) {
            throw AuthException.duplicateUsernameException();
        }

        // 일반 유저 생성
        String encodedPassword = passwordEncoder.encode(signupRequest.password());
        Member member = Member.createMember(signupRequest.username(), encodedPassword);
        Member savedMember = memberRepository.save(member);

        log.info("새 일반 유저 가입: {}", username);

        return SignupResponse.of(savedMember.getId(), savedMember.getUsername());
    }

    /**
     * 로그인
     */
    public SignInResponse signIn(SignInRequest signInRequest, HttpServletResponse response) {
        String username = signInRequest.username();
        Member member = memberRepository.findByUsername(username)
                .filter(m -> passwordEncoder.matches(signInRequest.password(), m.getPassword()))
                .orElseThrow(MemberException::invalidMemberException);

        // 토큰 생성, 저장
        String accessToken = saveRefreshToken(response, member);

        log.info("로그인 성공: {}", username);
        return SignInResponse.of(accessToken);
    }

    /**
     * 로그아웃
     */
    public void signOut(HttpServletResponse response) {
        Long memberId = JwtHelper.getMemberId();
        String memberName = JwtHelper.getMemberName();
        if (memberId == null || memberName == null) {
            throw AuthException.invalidTokenException();
        }
        // 토큰 삭제
        clearRefreshToken(memberId, response);
        // SecurityContextHolder 비우기
        clearSecurityContext();

        log.info("로그아웃 성공: {}", memberName);
    }

    /**
     * 토큰 재발급
     */
    public TokenReissueResponse reissueToken(String refreshToken, HttpServletResponse response) {
        // SecurityContextHolder 비우기
        clearSecurityContext();

        // refreshToken 검증
        validateRefreshToken(refreshToken, response);

        // redis 비교
        Long memberId = Long.parseLong(jwtHelper.getClaims(refreshToken, Claims::getSubject));
        String storedRefreshToken = refreshTokenRedisRepository.findBy(memberId);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            log.error("Redis에 저장된 refreshToken과 일치하지 않음: memberId={}", memberId);
            clearRefreshToken(memberId, response);
            throw AuthException.invalidTokenException();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::invalidMemberException);

        // 새 토큰 생성 및 저장
        String newAccessToken = saveRefreshToken(response, member);

        log.info("토큰 재발급 성공: {}", member.getUsername());

        return TokenReissueResponse.of(newAccessToken);
    }

    /**
     * refreshToken 검증
     */
    private void validateRefreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw AuthException.invalidTokenException();
        }
        try {
            jwtHelper.validateJwt(refreshToken);
        } catch (CustomAuthenticationException e) {
            jwtHelper.deleteRefreshTokenFromCookie(response);
            throw AuthException.invalidTokenException();
        }
    }

    /**
     * refreshToken 저장
     */
    private String saveRefreshToken(HttpServletResponse response, Member member) {
        String accessToken = jwtHelper.generateAccessToken(member.getId(), member.getUsername(), member.getRole());
        String refreshToken = jwtHelper.generateRefreshToken(member.getId());
        long expireTime = jwtHelper.getClaims(refreshToken, Claims::getExpiration).getTime();

        refreshTokenRedisRepository.save(member.getId(), refreshToken, expireTime);
        jwtHelper.saveRefreshTokenToCookie(response, refreshToken);

        return accessToken;
    }

    /**
     * refreshToken 삭제
     */
    private void clearRefreshToken(Long memberId, HttpServletResponse response) {
        // Redis에서 refreshToken 삭제
        refreshTokenRedisRepository.delete(memberId);
        // 쿠키에서 refreshToken 삭제
        jwtHelper.deleteRefreshTokenFromCookie(response);
    }

    /**
     * SecurityContextHolder 비우기
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
