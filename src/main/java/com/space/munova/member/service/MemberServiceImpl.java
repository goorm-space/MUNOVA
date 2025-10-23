package com.space.munova.member.service;

import com.space.munova.member.dto.GetMemberResponse;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.dto.UpdateMemberRequest;
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

    private final MemberRepository memberRepository;

    /**
     * 유저 정보 조회
     */
    public GetMemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        return GetMemberResponse.of(
                member.getUsername(),
                member.getAddress(),
                member.getRole()
        );
    }

    /**
     * 유저 정보 변경
     */
    @Transactional
    public void updateMember(Long memberId, UpdateMemberRequest updateMemberRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        member.updateMember(
                updateMemberRequest.username(),
                updateMemberRequest.address(),
                MemberRole.fromCode(updateMemberRequest.role())
        );
    }
}
