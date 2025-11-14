package com.space.munova.auth.service;

import com.space.munova.auth.dto.GenerateTokens;
import com.space.munova.auth.dto.SignInGenerateToken;
import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.auth.event.dto.SignupEvent;
import com.space.munova.core.annotation.RedisDistributeLock;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원가입
     */
    @RedisDistributeLock(key = "#signupRequest.username()")
    public void signup(SignupRequest signupRequest) {
        // 사용자명 중복체크
        if (memberRepository.existsByUsername(signupRequest.username())) {
            throw MemberException.duplicatedMemberName();
        }

        // 패스워드 encoding & 데이터베이스 저장 요청
        SignupEvent signupEvent = SignupEvent.from(signupRequest);
        eventPublisher.publishEvent(signupEvent);
    }

    /**
     * 로그인
     */
    public SignInGenerateToken signIn(SignInRequest signInRequest, String deviceId) {
        String username = signInRequest.username();
        Member member = memberRepository.findByUsername(username)
                .filter(m -> passwordEncoder.matches(signInRequest.password(), m.getPassword()))
                .orElseThrow(MemberException::invalidMemberException);

        // 토큰 생성, 저장
        GenerateTokens generateTokens = tokenService.saveRefreshTokenWithLock(member, deviceId);
        log.info("로그인 성공: {}", username);

        return SignInGenerateToken.of(
                member.getId(),
                member.getUsername(),
                generateTokens.accessToken(),
                generateTokens.refreshToken(),
                member.getRole()
        );
    }

    /**
     * 로그아웃
     */
    public void signOut(String deviceId, Long memberId) {
        // 토큰 삭제
        tokenService.clearRefreshToken(memberId, deviceId);
        // SecurityContextHolder 비우기
        tokenService.clearSecurityContext();

        log.info("로그아웃 성공: {}", memberId);
    }

}
