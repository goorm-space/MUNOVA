package com.space.munova.member.service;

import com.space.munova.auth.dto.GenerateTokens;
import com.space.munova.auth.service.TokenService;
import com.space.munova.core.annotation.RedisDistributeLock;
import com.space.munova.member.dto.GetMemberResponse;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.dto.UpdateMemberRequest;
import com.space.munova.member.dto.UpdateMemberResponse;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final TokenService tokenService;
    private final MemberRepository memberRepository;

    /**
     * 유저 정보 조회
     */
    @Override
    public GetMemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        return GetMemberResponse.of(
                member.getId(),
                member.getUsername(),
                member.getAddress(),
                member.getRole()
        );
    }

    /**
     * 유저 정보 변경
     */
    @Override
    @Transactional
    @RedisDistributeLock(key = "'MEMBER_UPDATE:' + #memberId")
    public UpdateMemberResponse updateMember(Long memberId, UpdateMemberRequest updateMemberRequest, String deviceId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        member.updateMember(
                updateMemberRequest.username(),
                updateMemberRequest.address(),
                MemberRole.fromCode(updateMemberRequest.role())
        );

        // 정보 변경 이후 모든 토큰 무효화
        tokenService.clearAllDeviceRefreshToken(memberId);
        // 업데이트된 정보를 바탕으로 토큰 발급
        GenerateTokens generateTokens = tokenService.saveRefreshToken(member, deviceId);
        return UpdateMemberResponse.of(generateTokens.accessToken(), generateTokens.refreshToken());
    }

    @Override
    public Member getMemberEntity(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);
    }
}
