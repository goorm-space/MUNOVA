package com.space.munova.member.service;

import com.space.munova.member.dto.GetMemberResponse;
import com.space.munova.member.dto.UpdateMemberRequest;

public interface MemberService {

    // 유저 정보 조회
    GetMemberResponse getMember(Long memberId);

    // 유저 정보 변경
    void updateMember(Long memberId, UpdateMemberRequest updateMemberRequest);
}
