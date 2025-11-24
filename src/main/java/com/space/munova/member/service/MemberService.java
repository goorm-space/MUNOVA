package com.space.munova.member.service;

import com.space.munova.member.dto.GetMemberResponse;
import com.space.munova.member.dto.UpdateMemberRequest;
import com.space.munova.member.dto.UpdateMemberResponse;
import com.space.munova.member.entity.Member;

public interface MemberService {

    // 유저 정보 조회
    GetMemberResponse getMember(Long memberId);

    // 유저 정보 변경
    UpdateMemberResponse updateMember(Long memberId, UpdateMemberRequest updateMemberRequest, String deviceId);

    Member getMemberEntity(Long memberId);
}
