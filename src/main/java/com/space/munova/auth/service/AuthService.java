package com.space.munova.auth.service;

import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignInResponse;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.auth.dto.SignupResponse;
import com.space.munova.auth.exception.AuthException;
import com.space.munova.auth.repository.RefreshTokenRedisRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.security.jwt.JwtHelper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public SignInResponse signIn(SignInRequest signInRequest) {
        String username = signInRequest.username();
        Member member = memberRepository.findByUsername(username)
                .filter(m -> passwordEncoder.matches(signInRequest.password(), m.getPassword()))
                .orElseThrow(MemberException::invalidMemberException);

        // 토큰 생성, 저장
        Long memberId = member.getId();
        String accessToken = jwtHelper.generateAccessToken(memberId, member.getRole());
        String refreshToken = jwtHelper.generateRefreshToken(memberId);
        long expireTime = jwtHelper.getClaims(refreshToken, Claims::getExpiration).getTime();

        refreshTokenRedisRepository.save(memberId, refreshToken, expireTime);

        log.info("로그인 성공: {}", username);
        return SignInResponse.of(accessToken, refreshToken);
    }


}
